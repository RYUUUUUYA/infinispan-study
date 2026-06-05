#!/bin/bash
# Infinispan データ分散テストスクリプト

BASE_URL_1="http://localhost:11222"
BASE_URL_2="http://localhost:11223"
BASE_URL_3="http://localhost:11224"
CACHE="distCache"
AUTH="admin:admin123"

echo "=== キャッシュ作成（既に存在する場合はスキップ） ==="
for PORT in 11222 11223 11224; do
  curl -s -o /dev/null -u $AUTH -X POST \
    "http://localhost:$PORT/rest/v2/caches/$CACHE" \
    -H "Content-Type: application/xml" \
    -d '<distributed-cache mode="SYNC" owners="2"/>'
done

echo ""
echo "=== データ投入 (node1 経由) ==="
for i in $(seq 1 10); do
  curl -s -o /dev/null -u $AUTH -X PUT \
    "$BASE_URL_1/rest/v2/caches/$CACHE/key-$i" \
    -H "Content-Type: text/plain" \
    -d "value-$i"
  echo "PUT key-$i -> node1"
done

echo ""
echo "=== 各ノードからキャッシュ統計を取得 ==="
for PORT in 11222 11223 11224; do
  echo "--- node (port: $PORT) ---"
  curl -s -u $AUTH \
    "http://localhost:$PORT/rest/v2/caches/$CACHE?action=stats" | python3 -m json.tool 2>/dev/null || \
  curl -s -u $AUTH \
    "http://localhost:$PORT/rest/v2/caches/$CACHE?action=stats"
  echo ""
done

echo ""
echo "=== ノード1 を停止して key-1 を取得（フェイルオーバー確認） ==="
echo "※ 手動で 'docker stop infinispan-node1' を実行後、以下で確認:"
echo "curl -u $AUTH $BASE_URL_2/rest/v2/caches/$CACHE/key-1"