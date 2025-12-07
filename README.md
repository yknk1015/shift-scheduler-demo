# シフトスケジューラー_v1

小規模〜中規模のコールセンターや店舗を想定した、**需要ベースのシフト自動生成ツール**です。  
「必要な席数」「従業員スキル」「休み・勤務不可」「週休ルール」などをまとめて考慮し、  
現場の負担を減らしながら、バランスの取れたシフトを自動で作成します。

実装は Java 17 / Spring Boot / SQLite で、文字コードはすべて UTF-8 に統一しています
個人開発のため、現在も鋭意機能追加中です。

## このアプリでできること


このツールは、例えば次のような現場を想定しています。

- 小〜中規模のコールセンター  
  （例：数十名規模・複数スキルのオペレーターが在籍）
- 店舗・窓口業務  
  （例：時間帯によって来客数が大きく変動する店舗）

「電話が多い時間帯は席数を厚く」「新人には簡単なスキルのみ」など、  
**需要の波とメンバー構成を両方見ながらシフトを組む場面**での活用をイメージしています。

## 画面イメージ（Screenshots）

### ログイン画面
![使い方1](https://github.com/user-attachments/assets/23ab539b-b902-48fd-a480-b3b6aca16e6c)

### ダッシュボード
![使い方2](https://github.com/user-attachments/assets/ccd7af86-d52b-4973-8bd5-d14113e5224f)

### シフト生成
![使い方3](https://github.com/user-attachments/assets/d99841a2-b526-4862-a08a-3ae9c5269cc9)

### 需要管理
![使い方4](https://github.com/user-attachments/assets/7656dcbf-03ec-4ac1-8cf9-e89455c96871)

### 従業員マスタ
![使い方5](https://github.com/user-attachments/assets/974ec616-577e-4877-8438-f50dc36dab35)


### 用語の簡単な説明

- **需要**：時間帯ごとの「必要な席数」のこと  
- **FREE（待機枠）**：その時間帯に余裕があり、別の業務に振り替えしやすい勤務  
- **OFF**：公休（お休み）  
- **有休**：有給休暇

README 内では、上記のような用語で統一しています。

---

## 主な画面

### ダッシュボード画面（/dashboard）

- 対象月の
  - シフト件数
  - 従業員数
  - 不足件数（必要席数に対して足りないシフト）
  をカード形式で表示します。
- 「シフト自動生成」「モニター」「需要 vs 供給ビュー」など、主要な画面への入口になっています。

### シフト自動生成・グリッド調整画面（/schedule-editor）

- 画面上部
  - 対象月を指定してシフトを自動生成
  - 既存シフトの初期化
  - CSV ダウンロード
- 画面下部
  - 「従業員 × 日付」のグリッド形式でシフトを表示
  - ドラッグ＆ドロップやコピー＆ペーストで、個別のシフト調整が可能
  - FREE / OFF / 有休 はフラグで切り替え可能
  - 昼休憩・小休憩を自動で提案する機能付き

### 運用モニター画面（/monitor）

- 日別に
  - FREE（待機枠）の数
  - OFF（公休）の数
  - 勤務日数メーター
  などを集計して表示します。
- 「誰がどれくらい出ているか」「FREE枠はどれくらい残っているか」を一覧で確認するのに使えます。

### 需要管理画面（/demand）

- 日次ビュー / 週次テンプレート / 月次プランをタブで切り替えできます。
- 「この日は繁忙日なので、席数を増やしたい」といった臨時需要の上乗せが可能です。
- タイムライン形式で、時間帯ごとの必要席数を編集できます。

### 需要 vs 供給ビュー（/demand-supply）

- 時間帯ごとに、以下をヒートマップで表示します。
  - 需要（必要席数）
  - 稼働中の人数
  - 休憩中の人数
  - FREE 勤務者によるフォロー可能枠
  - 不足人数
- 粒度は 15 / 30 / 60 分から選択できます。
- 「どの時間帯が一番足りていないか」「休憩の入り方でどれぐらい不足しているか」を把握するのに役立ちます。

---

## CSVエクスポートと活用イメージ

- 出力形式は **UTF-8 + BOM**、ヘッダーは日本語です。
- FREE / 休日プレースホルダーは `00:00-00:00` として出力されます。
- 含まれる項目（例）：
  - 日付・曜日
  - 区分（通常 / FREE / 休日 / 休暇）
  - 担当者名・スキル名
  - 稼働時間（分）
- そのまま Excel で開いて資料化したり、別システムへの取り込み用データとして利用できます。

---

## 操作を試してみる（Quick Try）

1. アプリケーションを起動します（起動方法は後述）。  
2. ブラウザで `http://localhost:8080/login` にアクセスし、管理者ユーザーでログインします。
3. メニューから「需要管理」（/demand）を開き、繁忙日などの臨時需要を追加します。
4. 「ダッシュボード」（/dashboard）で「最新シフト取得」を押し、対象月のサマリーが変化することを確認します。
5. 「需要 vs 供給ビュー」（/demand-supply）を開き、同じ期間でヒートマップがどう変わるかを確認します。
6. 必要に応じて `/schedule-editor` から個別にシフトを調整し、  
   `/api/schedule/export/csv` から CSV を取得して Excel で開きます。

---

## ここから下は開発者向けの情報です

システム構成や API、生成ロジックなどの詳細を記載しています。  
現場のご担当者様は、ここ以降を読み飛ばしていただいても問題ありません。

---

## 開発者向け情報

システム構成や API、生成ロジックなどを以下にまとめています。

### 起動方法 / Getting Started

```bash
mvn spring-boot:run
```

### 初期ユーザー（管理者）

- ユーザー名: admin
- パスワード: admin123

### 主な URL

- ログイン: http://localhost:8080/login
- ダッシュボード: http://localhost:8080/dashboard
- シフト自動生成・グリッド調整: http://localhost:8080/schedule-editor

### 生成ポリシー（概要）

- 需要（requiredSeats）を満たすよう、スキルが一致する従業員にシフトを割り当てます。
- 勤務不可（UNAVAILABLE）などの制約が付いた従業員は、その時間帯の割当対象外とします。
- 週休（weeklyRestDays）により、「週に何日働けるか」を制御します。同じ日に複数シフトがあっても、週休判定上は 1 日として扱います。
- すでに手動で登録済みのシフトは尊重し、同一時間帯の必要席数から差し引いて計算します。
- FREE 勤務（isFree=true）は、通常の供給には含めず、「不足や休憩による穴をフォローできる潜在枠」として別枠で扱います。

### 需要 vs 供給 計算ロジック（概要）

- 対象期間：指定した開始日〜終了日（1〜31日）
- 粒度：15 / 30 / 60 分（クエリパラメータ granularity）
- 需要側：DemandInterval を時間スロットに分解し、requiredSeats を集計
- 供給側：ShiftAssignment と BreakPeriod をもとに、休憩時間を差し引いた「実働 FTE」を算出
- FREE 勤務は通常の供給から除外し、「フォロー可能枠」として別集計
- UI では各時間帯ごとに、需要 / 稼働中（実働） / 休憩中 / FREE によるフォロー可能数 / 純粋な不足数などをヒートマップとサマリーカードで確認できます。

### 生成 API（管理者向け）

- すべて 管理者ロール（ROLE_ADMIN）かつ POST での呼び出しが必要です。
- URL をブラウザから GET した場合は 302 / 405 を返します。

**同期生成（月単位）**

```
POST /api/schedule/generate/demand?year=YYYY&month=M&granularity=60&reset=true|false
```

- `reset=true`: 既存のシフトをクリアして再生成
- `reset=false`: 既存のシフトは残し、空いている枠のみ自動で埋める

**同期生成（日単位）**

```
POST /api/schedule/generate/demand/day?date=YYYY-MM-DD&reset=true|false
```

**非同期生成（月単位）**

```
POST /api/schedule/generate/demand/async?year=YYYY&month=M&granularity=60&reset=true|false
```

### 参考 API

- 月次統計: `GET /api/schedule/stats/monthly?year=YYYY&month=M`
- 直近エラーログ: `GET /api/admin/error-logs?limit=50`

### 認証付き curl 例（PowerShell）

```powershell
# 1) ログイン（Cookie保存）
curl.exe -sS -i -c cookies.txt -H "Content-Type: application/json" `
  -d '{"username":"admin","password":"admin123"}' `
  -X POST http://localhost:8080/api/auth/login

# 2) 月の同期生成（空きのみ埋める）
curl.exe -sS -i -b cookies.txt -X POST `
  "http://localhost:8080/api/schedule/generate/demand?year=2025&month=11&granularity=60&reset=false"

# 3) 結果確認
curl.exe -sS -b cookies.txt `
  "http://localhost:8080/api/schedule?year=2025&month=11"
```

### デバッグ / Debugging

- `/admin-debug` から、生成・統計・スナップショットなどの管理者機能を実行できます。
- 例外は Global エラーハンドラおよび ErrorLogBuffer に記録されます。
- 詳細ログが必要な場合は、`application.properties` 等で次の設定を有効化してください。

```
logging.level.com.example.shiftv1.schedule=DEBUG
```

### 技術スタック / Stack

- Java 17, Spring Boot 3, Spring Security, Spring Data JPA
- SQLite
- Thymeleaf
- Maven

### 文字コード / Encoding

- テンプレート・CSV 出力ともに UTF-8 を使用しています。
- CSV は UTF-8 + BOM、日本語ヘッダーで出力されます。
