# Infinispan ローカル分散テスト環境

ローカル環境で Infinispan の複数ノードを立ち上げ、データ分散の動作を確認するための検証環境です。

---

## 構成

```
infinispan-study/
├── docker-compose.yml        # 3ノード構成の定義
├── infinispan-config.xml     # クラスター・キャッシュ設定
├── README.md
└── test/
    └── test_distribution.sh  # 分散動作確認スクリプト
```

### ノード構成

| コンテナ名 | ホストポート | 役割 |
|---|---|---|
| infinispan-node1 | 11222 | データ投入ノード |
| infinispan-node2 | 11223 | クラスターメンバー |
| infinispan-node3 | 11224 | クラスターメンバー |

### キャッシュ設定

| 項目 | 値 | 説明 |
|---|---|---|
| キャッシュ名 | `distCache` | 分散キャッシュ |
| モード | `DIST_SYNC` | 同期分散 |
| owners | `2` | 各エントリを2ノードに複製 |
| ノード検出 | `TCPPING` | Docker環境向け固定IPピアリング |

---

## 起動手順

```bash
# 1. クラスター起動
docker-compose up -d

# 2. ログでクラスター形成を確認（30秒ほど待つ）
docker-compose logs -f
# 以下のログが3ノード分出れば完了:
# ISPN000094: Received new cluster view ... (3 members)

# 3. クラスター状態確認
curl -u admin:admin123 http://localhost:11222/rest/v2/cluster
```

---

## テスト実行

```bash
bash test/test_distribution.sh
```

スクリプトは以下の順で動作します。

1. `distCache` キャッシュを作成
2. node1 経由でキー10件を投入（`key-1` 〜 `key-10`）
3. 各ノードからデータを取得し分散を確認
4. 各ノードのキャッシュ統計を表示

---

## 確認できること

### ハッシュ分散の確認

テスト実行後、各ノードの統計から以下を確認します。

```bash
curl -u admin:admin123 http://localhost:1122X/rest/v2/caches/distCache?action=stats
```

注目するフィールド：

| フィールド | 意味 |
|---|---|
| `approximate_entries_unique` | そのノードがプライマリオーナーのエントリ数 |
| `approximate_entries` | レプリカ含めた物理保持数 |
| `required_minimum_number_of_nodes` | データを失わないために必要な最低ノード数（owners と一致） |

**期待される結果：**

```
node1: approximate_entries_unique = 2
node2: approximate_entries_unique = 7
node3: approximate_entries_unique = 1
合計 = 10  ← 投入件数と一致（全エントリが分散保持されている）

approximate_entries 合計 = 5 + 9 + 6 = 20 = 10件 × owners(2)
```

### フェイルオーバーの確認

```bash
# node1 を停止
docker stop infinispan-node1

# node2 / node3 からデータを取得（owners=2 のため消えないはず）
curl -u admin:admin123 http://localhost:11223/rest/v2/caches/distCache/key-1
curl -u admin:admin123 http://localhost:11224/rest/v2/caches/distCache/key-1

# node1 を再起動（再参加後の再分散確認）
docker start infinispan-node1
```

---

## 統計フィールド一覧

| フィールド | 意味 |
|---|---|
| `approximate_entries` | 物理保持エントリ数（レプリカ含む） |
| `approximate_entries_unique` | プライマリオーナーのエントリ数（重複なし） |
| `current_number_of_entries` | 正確なエントリ数（分散キャッシュでは `-1` を返す仕様） |
| `stores` | 書き込み回数 |
| `retrievals` | 読み取り試行回数 |
| `hits` / `misses` | キャッシュヒット / ミス数 |
| `evictions` | メモリ不足による追い出し数 |
| `required_minimum_number_of_nodes` | データ保全に必要な最低ノード数 |
| `average_read_time` | 平均読み取り時間（ms） |
| `average_write_time` | 平均書き込み時間（ms） |

> `current_number_of_entries` が `-1` になるのは、分散キャッシュでは全ノードを集計しないと正確な値が出ないため、単一ノードの統計では `-1` を返す仕様です。

---

## 認証情報

| 項目 | 値 |
|---|---|
| ユーザー名 | `admin` |
| パスワード | `admin123` |

---

## トラブルシューティング

### `ISPN080052: The request authentication mechanism 'null' is not supported`

`infinispan-config.xml` の `<endpoint>` に BASIC 認証の明示指定が必要です。

```xml
<endpoint socket-binding="default" security-realm="default">
  <rest-connector>
    <authentication mechanisms="BASIC"/>
  </rest-connector>
</endpoint>
```

修正後はコンテナを再起動してください。

```bash
docker-compose down
docker-compose up -d
```

---

## 進捗

- [x] 3ノードクラスターの構築
- [x] 分散キャッシュへのデータ投入（REST API）
- [x] ハッシュ分散の統計確認
- [ ] フェイルオーバー確認（ノード停止時のデータ保全）
- [ ] ノード再参加時の再分散確認



以下は DistributedDemoTest の説明



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
