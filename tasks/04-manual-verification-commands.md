# AWS Infinispan 分散キャッシュ 実行用コマンド

このメモは、ECS が起動済みの状態で、ALB 経由の JSON API を手動で確認するためのコマンド集である。

前提:

- AWS profile は `jurabi-sandbox`
- リージョンは `ap-northeast-1`
- ALB の DNS 名は Terraform output か AWS コンソールで確認する
- サービスは最新イメージで再デプロイ済みであること

## 1. ALB の URL を設定する

```bash
export AWS_PROFILE=jurabi-sandbox
export AWS_REGION=ap-northeast-1
export ALB_BASE_URL='http://<ALBのDNS名>:9000'
```

## 2. 生存確認

```bash
curl -i "$ALB_BASE_URL/health"
```

期待:

- HTTP 200
- `status` が `UP`
- `node`、`owners`、`caches` を含む JSON

## 3. キャッシュへの新規追加

```bash
curl -i -X POST "$ALB_BASE_URL/api/get/CacheA/user-001" \
  -H 'Content-Type: application/json' \
  -d '{"id":"user-001","name":"alice","version":1}'
```

期待:

- HTTP 201
- 送った JSON がそのまま返る

## 4. キャッシュからの取得

```bash
curl -i "$ALB_BASE_URL/api/get/CacheA/user-001"
```

期待:

- HTTP 200
- `version: 1` の JSON が返る

## 5. キャッシュ更新

```bash
curl -i -X POST "$ALB_BASE_URL/api/get/CacheA/user-001" \
  -H 'Content-Type: application/json' \
  -d '{"id":"user-001","name":"alice","version":2}'
```

期待:

- HTTP 200
- `version: 2` の JSON が返る

## 6. 更新後の再取得

```bash
curl -i "$ALB_BASE_URL/api/get/CacheA/user-001"
```

期待:

- HTTP 200
- `version: 2` が返る

## 7. 複数キャッシュでの簡易確認

```bash
for cache in CacheA CacheB CacheC; do
  for i in 1 2 3; do
    curl -s -X POST "$ALB_BASE_URL/api/get/$cache/key-$i" \
      -H 'Content-Type: application/json' \
      -d "{\"cache\":\"$cache\",\"key\":\"key-$i\",\"version\":1}" >/dev/null
  done
done
```

```bash
for cache in CacheA CacheB CacheC; do
  for i in 1 2 3; do
    curl -s "$ALB_BASE_URL/api/get/$cache/key-$i"
    echo
  done
done
```

## 8. ALB の振り分けを確認する

同じ URL を連続で叩いて、CloudWatch Logs 側で `node=...` が分散しているかを見る。

```bash
for i in $(seq 1 20); do
  curl -s "$ALB_BASE_URL/api/get/CacheA/user-001" >/dev/null
done
```

## 9. CloudWatch Logs を追う

```bash
aws --profile jurabi-sandbox logs tail '/truncus-exp-multi-cluster/app' \
  --region ap-northeast-1 \
  --follow
```

見るログ:

- `event=infinispan_start`
- `event=cache_initialized`
- `event=cache_put`
- `event=cache_get`

## 10. 失敗時の確認ポイント

- `/health` が 200 を返すか
- ECS タスクが healthy か
- CloudWatch Logs に `BindException` が出ていないか
- `JGROUPS_BIND_ADDRESS` が古い値のまま残っていないか
- イメージが最新 digest に更新されているか

