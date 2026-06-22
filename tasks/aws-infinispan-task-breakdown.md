# AWS Infinispan 分散キャッシュ検証 タスク分解

## 目的

`AWSでのinfinispan分散キャッシュテスト.md` に記載された構成を実現し、AWS上で Embedded Infinispan を利用するアプリケーションを ECS にデプロイして、ALB 経由の JSON API から分散キャッシュの新規追加・取得・更新が期待通り動作することを確認する。

## 前提

- デプロイ作業は手動で行う。
- ローカルでは Maven ビルドと Docker イメージビルドができればよい。
- AWS の主要構成要素は ECR、ECS、ALB、CloudWatch Logs、S3 Ping 用 S3 バケットとする。
- ECS タスク数は各キャッシュの `owners` より多くする。
- 古い値が他ノードから一時的に読み取れることは許容する。

## タスク文書

| 分類 | 文書 | 主な成果物 |
|---|---|---|
| アプリケーション構築 | [01-application-build.md](./01-application-build.md) | JSON API、Embedded Infinispan 設定、Docker イメージ |
| インフラ構築 | [02-infrastructure-build.md](./02-infrastructure-build.md) | ECR、ECS、ALB、S3 Ping、CloudWatch Logs |
| 試験スクリプト作成及び実行 | [03-test-script-and-execution.md](./03-test-script-and-execution.md) | 試験スクリプト、試験データ、実行結果 |

## 全体依存関係

1. アプリケーション構築でコンテナ化可能な API アプリケーションを作成する。
2. インフラ構築で ECR、ECS、ALB、S3 Ping、ログ出力先を準備する。
3. アプリケーションイメージを ECR に push し、ECS サービスとして起動する。
4. 試験スクリプトから ALB エンドポイントへアクセスし、分散キャッシュ動作を確認する。

## 完了条件

- ALB 経由で複数 ECS タスクにリクエストが振り分けられる。
- 複数キャッシュが Distributed Cache として構成される。
- 各キャッシュで新規追加、取得、更新の API が JSON で動作する。
- 十分な件数の書き込み後、複数回の読み出しで期待値を取得できる。
- 十分な件数の更新後、複数回の読み出しで最終的に期待値を取得できる。
- API の新規追加、取得、更新ログが CloudWatch Logs に出力される。
