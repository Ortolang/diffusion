#!/bin/ash

cat > payload.json <<__HERE__
{
  "project": "${DTRACK_PROJECT_ID}",
  "bom": "$(cat target/bom.xml |base64 -w 0 - )"
}
__HERE__

curl -k -X "PUT" "${DTRACK_URL}/api/v1/bom" \
     -H 'Content-Type: application/json' \
     -H "Host: dtrack.ortolang.fr" \
     -H "X-API-Key: ${DTRACK_AUTH_TOKEN}" \
     -d @payload.json