const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, PUT, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Authorization",
  "Access-Control-Max-Age": "86400"
};

function response(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { ...cors, "Content-Type": "application/json; charset=utf-8" }
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: cors });
    }

    if (url.pathname === "/health" && request.method === "GET") {
      return response({ ok: true, service: "ARIF LIVE TV channel sync" });
    }

    if (url.pathname !== "/channels") {
      return response({ error: "Not found" }, 404);
    }

    if (request.method === "GET") {
      const channels = await env.ARIF_TV_CHANNELS.get("channels", "json") || [];
      return response({ channels });
    }

    if (request.method === "PUT") {
      const auth = request.headers.get("Authorization") || "";
      if (!env.ADMIN_TOKEN || auth !== `Bearer ${env.ADMIN_TOKEN}`) {
        return response({ error: "Unauthorized" }, 401);
      }

      const raw = await request.text();
      if (raw.length > 20 * 1024 * 1024) {
        return response({ error: "Payload too large" }, 413);
      }

      let body;
      try { body = JSON.parse(raw); }
      catch { return response({ error: "Invalid JSON" }, 400); }

      if (!body || !Array.isArray(body.channels) || body.channels.length > 500) {
        return response({ error: "channels must be an array (maximum 500)" }, 400);
      }

      await env.ARIF_TV_CHANNELS.put("channels", JSON.stringify(body.channels));
      return response({ ok: true, count: body.channels.length });
    }

    return response({ error: "Method not allowed" }, 405);
  }
};
