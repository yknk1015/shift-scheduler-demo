# Shift Scheduler Demo
小〜中規模コールセンターや店舗を想定した **需要ベースのシフト自動生成ツール** です。  
Java 17 / Spring Boot / SQLite で構築し、  
「需要（必要席数）」「従業員スキル」「制約・休暇」「週休ルール」などを考慮してシフトを組みます。  
文字コードはすべて UTF-8 で統一しています。

## このアプリでできること（What it does）

- 需要ブロック（開始時刻 / 終了時刻 / 必要席数 / 必要スキル）から、**自動でシフトを割り当て**
- 従業員ごとの
  - スキル
  - 勤務不可（UNAVAILABLE）
  - 勤務時間制限（LIMITED）
  - 休暇（VACATION / SICK / PERSONAL）
  を考慮して割当
- 午前・午後などの **短時間シフトを自動ペアリング** してフルタイム勤務に統合（任意）
- 管理者は **同期 / 非同期の生成 API** を利用して、バッチ / 手動どちらからでもシフトを更新
- 需要と実際のシフト（供給）を、時間帯ごとにヒートマップで比較

## 概要 / Overview

- 需要（Demand）
  - 「開始」「終了」「必要席数」「必要スキル」で定義
- 従業員（Employee）
  - 複数スキルと、勤務制約フラグ（UNAVAILABLE / LIMITED / VACATION / SICK / PERSONAL）を保持
- シフト生成
  - 需要 requiredSeats を満たすよう、スキル適合者に割当
  - 制約が付いている従業員は対象外
  - 週休（weeklyRestDays）により、週あたり勤務可能日数を制御
  - 既存の手動割当は尊重し、同一時間帯は座席数から差し引き
- プレースホルダ
  - FREE（待機枠） / OFF / 有休 は専用フラグで管理し、需要との差分分析時に別計上
  
## 画面構成 / UI Highlights

- **ダッシュボード (`/dashboard`)**
  - 対象月、シフト件数、従業員数、不足件数をカードで表示
  - 「シフト生成」「モニタ」「需要 vs 供給ビュー」など、主要画面への導線を集約

- **シフト自動生成 + グリッド調整 (`/schedule-editor`)**
  - 上部：対象月のシフトを自動生成・初期化・CSVダウンロード
  - 下部：従業員 × 日付のグリッドで、ドラッグ＆ドロップ / コピー＆ペーストによる個別調整
  - FREE / OFF / 有休 はフラグで切替可能、休憩（昼休憩・小休憩）の追加・自動提案機能付き

- **運用モニター (`/monitor`)**
  - 日別の FREE / OFF 数、勤務日数メーターなどを集計表示
  - 「誰がどれくらい出ているか」「FREE枠はどれぐらいあるか」を一覧で確認

- **需要管理 (`/demand`)**
  - 日次ビュー / 週次テンプレート / 月次プランをタブで切り替え
  - 繁忙日のみ需要を上乗せする「臨時需要ブースト」や、タイムライン形式の編集 UI を搭載

- **需要 vs 供給ビュー (`/demand-supply`)**
  - 時間帯ごとに「需要 / 稼働中 / 休憩中 / FREEフォロー可能枠 / 不足」をヒートマップで表示
  - 粒度は 15 / 30 / 60 分を選択可能

- **CSVエクスポート**
  - UTF-8+BOM、日本語ヘッダー
  - FREE / 休日プレースホルダは `00:00-00:00` で出力
  - 曜日・区分（通常 / FREE / 休日 / 休暇）・スキル名・稼働時間(分)を含み、そのまま資料や別システムへ連携可能

## 使い方（ブラウザから試す） / Quick Try

1. `http://localhost:8080/demand`  
   → 臨時需要を追加して、特定日の必要席数を増やす
2. `http://localhost:8080/dashboard`  
   → 「最新シフト取得」で対象月のサマリーが更新されることを確認
3. `http://localhost:8080/demand-supply`  
   → 同じ期間を指定し、需要 vs 供給のヒートマップが変化するか確認  
4. `POST /api/schedule/export/csv`  
   → CSV を取得し、Excel で文字化けなく開けることを確認
   
## 生成ポリシー / Generation Policy

- 需要 requiredSeats を満たすよう、スキル適合者に割当
- 制約（UNAVAILABLE など）は割当不可
- 週休（weeklyRestDays）: 週の勤務可能日数 = 7 − 週休日数（同日の複数枠は1日扱い）
- 既存の手動割当は尊重し、同一時間帯は座席数から差し引く

## 生成API / Stable APIs（POST / 要管理者）

以下はすべて管理者（ROLE_ADMIN）かつ POST での呼出が必要です。URL 直打ち GET は 302/405 になります。

- 同期生成（月）  
  `POST /api/schedule/generate/demand?year=YYYY&month=M&granularity=60&reset=true|false`  
  reset=true: 既存をクリアして再生成／reset=false: 既存を残し、空きのみ埋める
- 同期生成（日）  
  `POST /api/schedule/generate/demand/day?date=YYYY-MM-DD&reset=true|false`
- 非同期生成（月） 
  `POST /api/schedule/generate/demand/async?year=YYYY&month=M&granularity=60&reset=true|false`

参考API:

- 月次統計: `GET /api/schedule/stats/monthly?year=YYYY&month=M`
- 直近エラーログ: `GET /api/admin/error-logs?limit=50`

### 認証付きの curl 例（PowerShell）

```
# 1) ログイン（Cookie保存）
curl.exe -sS -i -c cookies.txt -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' -X POST http://localhost:8080/api/auth/login

# 2) 月の同期生成（空きのみ埋める）
curl.exe -sS -i -b cookies.txt -X POST "http://localhost:8080/api/schedule/generate/demand?year=2025&month=11&granularity=60&reset=false"

# 3) 結果確認
curl.exe -sS -b cookies.txt "http://localhost:8080/api/schedule?year=2025&month=11"
```

## デバッグ / Debugging

- `/admin-debug` で生成/統計/スナップショット等を実行可能（管理者）
- 例外は Global エラーハンドラと ErrorLogBuffer に記録。必要に応じて  
  `logging.level.com.example.shiftv1.schedule=DEBUG` を有効化

## 文字コード / Encoding

テンプレートは UTF-8 固定です。

## 起動 / Getting Started

```
mvn spring-boot:run
```

初期ユーザー（管理者）:

- `admin` / `admin123`

主なURL:

- ログイン: http://localhost:8080/login
- ダッシュボード: http://localhost:8080/dashboard
- シフト自動生成 / グリッド調整: http://localhost:8080//schedule-editor

## 需要 vs 供給計算ロジック（概要）

- 対象期間: start〜end（1〜31日）
- 粒度: 15 / 30 / 60 分（クエリパラメータ granularity）
- 需要側:DemandInterval を時間スロットに分解し、requiredSeats を集計
- 供給側:ShiftAssignment と BreakPeriod を参照し、休憩中の重複分を差し引いた 実稼働 FTE を算出
- FREE 勤務（isFree=true）は通常供給には含めず、「休憩不足・純不足をフォローできる潜在枠」として別配列で返却

- UI では以下を確認できます：
- 各時間帯ごとの「需要 / 稼働供給 / 休憩中 / 不足（休憩由来 or 純不足）」のヒートマップ
- FREE 勤務者によるフォロー可能数・純不足に転用可能な残り枠
- 最大不足時間帯や参照されたシフト件数などのサマリーカード
- `/schedule-editor` のヘッダーからも新ビューへのショートカットを追加しています。

## スタック / Stack

- Java 17, Spring Boot 3, Spring Security, Spring Data JPA
- SQLite, Thymeleaf, Maven

## 技術メモ

- 生成は @Transactional で整合性を確保。問題があれば ErrorLogBuffer / DEBUG ログで診断可能。
- CSVエクスポートは UTF-8+BOM・日本語ヘッダーで出力され、FREE/休日プレースホルダーは `00:00-00:00` で統一しています。
  - 曜日・区分（通常/FREE/休日/休暇）・担当者スキル・稼働時間(分)を含むため、外部レポートにも流用可能です。
