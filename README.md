# infinispan-study

Infinispan の学習・検証用リポジトリです。

## 環境

* Java 17
* Eclipse
* Maven

##Infinispan構成

本プロジェクトでは Infinispan を Embedded Mode で利用しています。

そのため、Infinispan Server や Docker コンテナの構築は不要です。
Maven依存関係を追加し、infinispan.xml に設定を記述するだけで利用できます。

データはアプリケーション内のメモリ上で管理されます

## セットアップ / 実行方法

1. Eclipse にプロジェクトを Import
2. Maven の依存関係を更新
3. `src/test/java/demo/DistributedDemoTest.java` を右クリック
4. `Run As` → `JUnit Test` を実行

## 内容

### DistributedDemoTest

Infinispan の基本動作確認用テストです。

### Person

テスト用エンティティクラスです。

### DemoSchema

ProtoStream 用スキーマ定義です。
