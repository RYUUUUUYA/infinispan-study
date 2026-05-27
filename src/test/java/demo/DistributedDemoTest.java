package demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.IndexStorage;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.QueryFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DistributedDemoTest {

    // ── 2つのノード（＝2つの倉庫）を表す変数 ──
    static DefaultCacheManager node1;
    static DefaultCacheManager node2;

    // ── 各ノードから見たキャッシュ（＝各倉庫の窓口）──
    static Cache<String, Person> cache1;
    static Cache<String, Person> cache2;

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 全テストの前に1回だけ実行される準備処理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @BeforeAll
    static void setup() {
        System.out.println("\n========== 準備開始 ==========");

        // 2つのノードを起動する
        System.out.println("● Node1 を起動中...");
        node1 = createNode("Node1");
        System.out.println("● Node1 起動完了");

        System.out.println("● Node2 を起動中...");
        node2 = createNode("Node2");
        System.out.println("● Node2 起動完了");

        // 各ノードから "people" キャッシュを取得する
        cache1 = node1.getCache("people");
        cache2 = node2.getCache("people");

        // Node1の窓口からデータを4件投入する
        // （Infinispanが内部で自動的に2ノードに分散して保管する）
        System.out.println("● データ投入中（Node1の窓口から4件）...");
        cache1.put("1", new Person("Alice", 30, "Tokyo"));
        cache1.put("2", new Person("Bob",   25, "Osaka"));
        cache1.put("3", new Person("Carol", 35, "Tokyo"));
        cache1.put("4", new Person("Dave",  28, "Nagoya"));
        System.out.println("● データ投入完了");

        System.out.println("========== 準備完了 ==========\n");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // 全テストの後に1回だけ実行される後片付け処理
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @AfterAll
    static void teardown() {
        System.out.println("\n========== 後片付け ==========");
        node1.stop();
        node2.stop();
        System.out.println("========== 後片付け完了 ==========");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // テスト1: どちらのノードから検索しても同じ結果が返る
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Test
    @DisplayName("どちらのノードから検索しても同じ結果が返る")
    void queryFromEitherNodeReturnsSameResults() {
        System.out.println("---------- テスト1 開始 ----------");

        // ── Node1の窓口で検索の準備をする ──
        QueryFactory qf1 = Search.getQueryFactory(cache1);

        // ── Node2の窓口で検索の準備をする ──
        QueryFactory qf2 = Search.getQueryFactory(cache2);

        // ── Node1の窓口から「cityがTokyoの人」を検索する ──
        System.out.println("● Node1から city='Tokyo' で検索...");
        List<Person> resultFromNode1 = qf1.<Person>create(
            "FROM demo.Person WHERE city = 'Tokyo'"
        ).execute().list();
        System.out.println("  結果: " + resultFromNode1.size() + "件");
        resultFromNode1.forEach(p ->
            System.out.println("    " + p.name() + " (age=" + p.age() + ")"));

        // ── Node2の窓口から、まったく同じ条件で検索する ──
        System.out.println("● Node2から city='Tokyo' で検索...");
        List<Person> resultFromNode2 = qf2.<Person>create(
            "FROM demo.Person WHERE city = 'Tokyo'"
        ).execute().list();
        System.out.println("  結果: " + resultFromNode2.size() + "件");
        resultFromNode2.forEach(p ->
            System.out.println("    " + p.name() + " (age=" + p.age() + ")"));

        // ── 検証1: Node1からの検索結果が2件であること ──
        assertEquals(2, resultFromNode1.size(),
            "Node1からの検索で2件取得できるはず");

        // ── 検証2: Node2からの検索結果も2件であること ──
        assertEquals(2, resultFromNode2.size(),
            "Node2からの検索でも2件取得できるはず");

        // ── 検証3: 両方の結果から名前を取り出して、ソートして比較する ──
        List<String> namesFromNode1 = resultFromNode1.stream()
            .map(Person::name)   // 各Personから名前だけ取り出す
            .sorted()            // アルファベット順に並べる
            .toList();           // リストにする

        List<String> namesFromNode2 = resultFromNode2.stream()
            .map(Person::name)
            .sorted()
            .toList();

        // ── 検証4: 名前の中身が正しいこと（AliceとCarol）──
        assertEquals(List.of("Alice", "Carol"), namesFromNode1,
            "Tokyo在住はAliceとCarolのはず");

        // ── 検証5: Node1とNode2の結果が完全に一致すること ──
        assertEquals(namesFromNode1, namesFromNode2,
            "どちらのノードからでも同じ結果が返るはず");

        System.out.println("● 検証OK: 両ノードから同じ結果が返った");
        System.out.println("---------- テスト1 完了 ----------");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // テスト2: データの分布状況をコンソールで確認する（学習用）
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Test
    @DisplayName("データの分布状況をコンソールで確認する（学習用）")
    void showDataDistribution() {
        System.out.println("---------- テスト2 開始（分布確認）----------");

        // ── 20件のデータをNode1の窓口から投入する ──
        String[] cities = {"Tokyo", "Osaka", "Nagoya", "Fukuoka", "Sapporo"};
        for (int i = 100; i < 120; i++) {
            String city = cities[i % cities.length];
            cache1.put("dist-" + i, new Person("Person" + i, 20 + (i % 30), city));
        }
        System.out.println("● 20件投入完了");

        // ── 各キーがどちらのノードに保管されているか調べる ──
        var topo1 = cache1.getAdvancedCache().getDistributionManager().getCacheTopology();

        int node1Count = 0;
        int node2Count = 0;

        for (int i = 100; i < 120; i++) {
            String key = "dist-" + i;
            boolean onNode1 = topo1.isReadOwner(key);
            Person p = cache1.get(key);
            String owner = onNode1 ? "Node1" : "Node2";

            System.out.printf("  key=%-8s (%s, %s) → %s%n",
                key, p.name(), p.city(), owner);

            if (onNode1) node1Count++;
            else node2Count++;
        }

        // ── 集計結果を表示する ──
        System.out.println();
        System.out.printf("● 集計: Node1=%d件, Node2=%d件%n", node1Count, node2Count);
        System.out.println("---------- テスト2 完了（分布確認）----------");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // テスト3: マルチスレッドで1000件投入しても矛盾なく保存・検索できる
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    @Test
    @DisplayName("マルチスレッドで1000件投入しても矛盾なく保存・検索できる")
    void concurrentPutAndQuery() throws Exception {
        System.out.println("---------- テスト3 開始（マルチスレッド）----------");

        int totalItems = 1000;
        int threadCount = 4;
        int itemsPerThread = totalItems / threadCount;

        // ── 4つのスレッドで同時にデータを投入する ──
        //    スレッド0: mt-0 ～ mt-249
        //    スレッド1: mt-250 ～ mt-499
        //    スレッド2: mt-500 ～ mt-749
        //    スレッド3: mt-750 ～ mt-999
        var threads = new Thread[threadCount];
        var errors = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            int start = t * itemsPerThread;
            int end = start + itemsPerThread;

            // ── スレッドごとにNode1とNode2を交互に使う ──
            Cache<String, Person> cache = (t % 2 == 0) ? cache1 : cache2;
            String threadName = "Thread-" + t;

            threads[t] = new Thread(() -> {
                try {
                    for (int i = start; i < end; i++) {
                        cache.put("mt-" + i, new Person(
                            "Person" + i,
                            20 + (i % 50),
                            "City" + (i % 10)
                        ));
                    }
                    System.out.printf("  %s: mt-%d ～ mt-%d 投入完了%n",
                        threadName, start, end - 1);
                } catch (Exception e) {
                    System.out.println("  " + threadName + ": エラー発生 → " + e.getMessage());
                    errors.incrementAndGet();
                }
            });
        }

        // ── 全スレッドを開始する ──
        System.out.println("● 4スレッドで1000件を同時投入中...");
        for (var thread : threads) thread.start();

        // ── 全スレッドの完了を待つ ──
        for (var thread : threads) thread.join();

        // ── 検証1: 投入中にエラーが発生していないこと ──
        assertEquals(0, errors.get(), "投入中にエラーが発生していないはず");

        // ── 検証2: 1000件すべてが保存されていること ──
        int count = 0;
        for (int i = 0; i < totalItems; i++) {
            Person p = cache1.get("mt-" + i);
            if (p != null) count++;
        }
        System.out.printf("● 保存確認: %d / %d 件%n", count, totalItems);
        assertEquals(totalItems, count, "1000件すべて保存されているはず");

        // ── 検証3: Node2からクエリしても正しい件数が返ること ──
        //    City0 に該当するのは i % 10 == 0 → 0,10,20,...,990 → 100件
        var result = cache2.query("FROM demo.Person WHERE city = 'City0'")
            .execute().list();
        System.out.printf("● クエリ確認: city='City0' → %d件%n", result.size());
        assertEquals(100, result.size(),
            "City0 は1000件中100件あるはず");

        System.out.println("---------- テスト3 完了（マルチスレッド）----------");
    }

    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    // ノードを1つ作るための共通メソッド
    // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    static DefaultCacheManager createNode(String nodeName) {

        // ── クラスタ全体の設定 ──
        // 「demo-cluster」という名前のクラスタに参加させる
        var global = new GlobalConfigurationBuilder();
        global.transport()
            .defaultTransport()
            .clusterName("demo-cluster")
            .nodeName(nodeName);

        // ── Personクラスのシリアライザを登録する ──
        global.serialization()
            .addContextInitializer(new DemoSchemaImpl());

        var globalConfig = global.build();

        // ── キャッシュ（データの保管場所）の設定 ──
        var cacheConfig = new ConfigurationBuilder()
            .clustering()
                // DIST_SYNC = データを複数ノードに分散して保管するモード
                .cacheMode(CacheMode.DIST_SYNC)
                // numOwners(1) = 各データは1つのノードだけが持つ
                .hash().numOwners(1)
            .indexing()
                // 検索機能を有効にする
                .enable()
                // インデックス（検索用の索引）をメモリ上に置く
                .storage(IndexStorage.LOCAL_HEAP)
                // Person クラスを検索対象として登録する
                .addIndexedEntity(Person.class)
            .build();

        // ── 設定をもとにノードを起動する ──
        var manager = new DefaultCacheManager(globalConfig);

        // ── "people" という名前でキャッシュを登録する ──
        manager.defineConfiguration("people", cacheConfig);

        return manager;
    }
}