# Embedded Infinispan API アプリケーション

## 概要

Spring Boot で Embedded Infinispan を起動し、JSON API から `CacheA`、`CacheB`、`CacheC` へ任意 JSON を保存する。

## API

```text
GET  /api/get/{cacheName}/{key}
POST /api/get/{cacheName}/{key}
GET  /health
```

`POST` は `Content-Type: application/json` の任意 JSON を受け取る。キー未存在は `404`、不明なキャッシュ名と不正 JSON は `400`、内部エラーは `500` を返す。

## 主な環境変数

| 変数 | 既定値 | 説明 |
|---|---|---|
| `PORT` | `8080` | HTTP ポート |
| `CACHE_NAMES` | `CacheA,CacheB,CacheC` | 起動時に作成するキャッシュ |
| `CACHE_OWNERS` | `2` | Distributed Cache の owner 数 |
| `INFINISPAN_CLUSTER_NAME` | `aws-infinispan-demo` | クラスター名 |
| `TASK_ID` | ホスト名 | ログに出すタスク識別子 |
| `INFINISPAN_DISCOVERY` | `local` | `local` または `s3` |
| `S3_PING_BUCKET` | なし | S3 Ping 用バケット。内部的に `jgroups.aws.s3.bucket_name` へ反映する |
| `S3_PING_PREFIX` | `infinispan-demo` | S3 Ping 用プレフィックス。内部的に `jgroups.aws.s3.bucket_prefix` へ反映する |
| `AWS_REGION` | `ap-northeast-1` | AWS リージョン。内部的に `jgroups.aws.s3.region_name` へ反映する |
| `JGROUPS_BIND_ADDRESS` | `0.0.0.0` | JGroups bind address |
| `JGROUPS_BIND_PORT` | `7800` | JGroups bind port |
| `JGROUPS_TCPPING_INITIAL_HOSTS` | `127.0.0.1[7800]` | local discovery 用 initial hosts |

## ローカル起動

```bash
mvn spring-boot:run
```

## Docker ビルド

```bash
docker build -t infinispan-demo:local .
docker run --rm -p 8080:8080 -e INFINISPAN_DISCOVERY=local infinispan-demo:local
```

## 動作確認

```bash
curl http://localhost:8080/health
curl -i -X POST http://localhost:8080/api/get/CacheA/key-1 \
  -H 'Content-Type: application/json' \
  -d '{"value":"created"}'
curl -i http://localhost:8080/api/get/CacheA/key-1
curl -i -X POST http://localhost:8080/api/get/CacheA/key-1 \
  -H 'Content-Type: application/json' \
  -d '{"value":"updated"}'
```
