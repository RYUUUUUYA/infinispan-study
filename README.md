# infinispan-study

Infinispan の学習・検証用リポジトリです。

## 環境

* Java 17
* Eclipse
* Maven

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
