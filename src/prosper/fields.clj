(ns prosper.fields
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clj-time.core :refer [now]]
            [clojure.set :refer [difference]]
            [clj-time.coerce :refer [to-timestamp]]
            [clojure.tools.logging :as log]
            [prosper.query :as q]))

(def fields
  {:RTL005 "integer"
   :ChannelCode "varchar(120)"
   :ILN501 "integer"
   :CreditPullDate "timestamp with time zone"
   :ALL906 "integer"
   :REV905 "integer"
   :BorrowerListingDescription "varchar(240)"
   :ILN908 "integer"
   :REV077 "integer"
   :AmountRemaining "double precision"
   :ListingMonthlyPayment "double precision"
   :ALL114 "integer"
   :BAC159 "integer"
   :ILN005 "integer"
   :BAC908 "integer"
   :ALL144 "integer"
   :ALE905 "integer"
   :REV201 "integer"
   :WholeLoanStartDate "timestamp with time zone"
   :ALL136 "integer"
   :REV002 "integer"
   :RTR590 "integer"
   :ILN403 "integer"
   :ALL076 "integer"
   :ILN127 "integer"
   :ALL601 "integer"
   :ALL503 "integer"
   :REP302 "integer"
   :ALL805 "integer"
   :REP906 "integer"
   :BAC751 "integer"
   :BAC904 "integer"
   :ListingTerm "integer"
   :REV106 "integer"
   :ILN067 "integer"
   :ALL007 "integer"
   :BAC075 "integer"
   :BAC078 "integer"
   :BAC403 "integer"
   :REP081 "integer"
   :BAC901 "integer"
   :FICOScore "varchar(8)"
   :ALE075 "integer"
   :ALL904 "integer"
   :RTR076 "integer"
   :ALL006 "integer"
   :ILN007 "integer"
   :REV117 "integer"
   :GroupIndicator "boolean"
   :ILN906 "integer"
   :EstimatedReturn "double precision"
   :NowDelinquentDerog "integer"
   :ILN720 "integer"
   :ALE501 "integer"
   :ILN109 "integer"
   :ALE804 "integer"
   :RTL905 "integer"
   :ALL082 "integer"
   :ALL208 "integer"
   :ALE601 "integer"
   :ILN130 "integer"
   :RTR077 "integer"
   :CRU001 "integer"
   :ILN105 "integer"
   :BAC023 "integer"
   :BAC077 "integer"
   :REV075 "integer"
   :RTL077 "integer"
   :REV114 "integer"
   :ALL010 "integer"
   :REP002 "integer"
   :ILN502 "integer"
   :ALL118 "integer"
   :ILN108 "integer"
   :ILN504 "integer"
   :ALL102 "integer"
   :RTI026 "integer"
   :REV584 "integer"
   :ALL085 "integer"
   :CAP026 "integer"
   :ALL202 "integer"
   :RTR908 "integer"
   :ALL143 "integer"
   :ILN074 "integer"
   :ListingStatusDescription "varchar(120)"
   :ALE502 "integer"
   :REV064 "integer"
   :ALL023 "integer"
   :RealEstatePayment "double precision"
   :ALL106 "integer"
   :ALL103 "integer"
   :BAC081 "integer"
   :REV108 "integer"
   :BAC752 "integer"
   :TotalInquiries "integer"
   :BAC302 "integer"
   :LoanNumber "timestamp with time zone"
   :BAC084 "integer"
   :IncomeVerifiable "boolean"
   :ALE022 "integer"
   :ILN804 "integer"
:ALL125 "integer"
:RTR906 "integer"
:ILN122 "integer"
:RTL502 "integer"
:REV084 "integer"
:ALL092 "integer"
:BorrowerState "varchar(16)"
:REV111 "integer"
:BAC502 "integer"
:ALL701 "integer"
:REV102 "integer"
:BAC002 "integer"
:REV022 "integer"
:FIL022 "integer"
:REV076 "integer"
:AmountParticipation "double precision"
:ILN023 "integer"
:REV116 "integer"
:ALL127 "integer"
:ILN117 "integer"
:REV125 "integer"
:ALL116 "integer"
:REP080 "integer"
:ALL201 "integer"
:REV503 "integer"
:ALL703 "integer"
:REV080 "integer"
:REV701 "integer"
:BorrowerAPR "double precision"
:ILN201 "integer"
:REV903 "integer"
:GBL007 "integer"
:REV007 "integer"
:ILN125 "integer"
:ILN701 "integer"
:BAC501 "integer"
:ProsperScore "integer"
:ALL128 "integer"
:ALL152 "integer"
:PriorProsperLoansLateCycles "timestamp with time zone"
:REV006 "integer"
:MaxPriorProsperLoan "timestamp with time zone"
:ALL145 "integer"
:RTL080 "integer"
:ALL141 "integer"
:PartialFundingIndicator "boolean"
:ALE908 "integer"
:REV906 "integer"
:RTR028 "integer"
:RTR026 "integer"
:REV122 "integer"
:BAC905 "integer"
:ALE077 "integer"
:DelinquenciesOver90Days "integer"
:ALL110 "integer"
:ALL207 "integer"
:ALL026 "integer"
:ALL062 "integer"
:ALL003 "integer"
:REP076 "integer"
:ALL113 "integer"
:ILN116 "integer"
:REP001 "integer"
:REP078 "integer"
:LoanOriginationDate "timestamp with time zone"
:ALL078 "integer"
:PercentFunded "double precision"
:REV105 "integer"
:ALL119 "integer"
:BAC080 "integer"
:ALL780 "integer"
:ALL111 "integer"
:ALL124 "integer"
:ILN080 "integer"
:CAP801 "integer"
:RTR589 "integer"
:REP074 "integer"
:REP903 "integer"
:RTR159 "integer"
:ILN110 "integer"
:RTR084 "integer"
:ILN086 "integer"
:AmountDelinquent "double precision"
:PublicRecordsLast10Years "integer"
:REV127 "integer"
:ALL403 "integer"
:REP077 "integer"
:REP904 "integer"
:RTL074 "integer"
:ILN128 "integer"
:ALL005 "integer"
:CurrentCreditLines "integer"
:ILN104 "integer"
:ILN129 "integer"
:PriorProsperLoansPrincipalBorrowed "integer"
:ALE023 "integer"
:PriorProsperLoanEarliestPayOff "integer"
:ALE002 "integer"
:ALL071 "integer"
:ILN111 "integer"
:HEQ001 "integer"
:RTR022 "integer"
:RTR035 "integer"
:RTL906 "integer"
:ALE074 "integer"
:ALL131 "integer"
:BAC584 "integer"
:REV126 "integer"
:ALL142 "integer"
:RTL503 "integer"
:ILN085 "integer"
:REV404 "integer"
:ALL130 "integer"
:REV112 "integer"
:ListingStatus "integer"
:BAC005 "integer"
:CurrentDelinquencies "integer"
:BAC071 "integer"
:REV115 "integer"
:FundingThreshold "double precision"
:BAC037 "integer"
:ALL081 "integer"
:ListingCreationDate "timestamp with time zone"
:BRR026 "integer"
:ILN102 "integer"
:RTL904 "integer"
:ALL077 "integer"
:BAC401 "integer"
:IncomeRangeDescription "varchar(100)"
:PriorProsperLoansOnTimePayments "integer"
:Occupation "varchar(100)"
:DTIwProsperLoan "double precision"
:OpenCreditLines "integer"
:REV026 "integer"
:FIL023 "integer"
:IsHomeowner "boolean"
:DelinquenciesOver30Days "integer"
:BAC303 "integer"
:REV585 "integer"
:ALL090 "integer"
:ListingRequestAmount "double precision"
:FIL001 "integer"
:ILN106 "integer"
:ALE801 "integer"
:ILN084 "integer"
:EffectiveYield "double precision"
:ILN702 "integer"
:REP601 "integer"
:ALL117 "integer"
:ALL504 "integer"
:RTL081 "integer"
:REP901 "integer"
:ALE403 "integer"
:ILN801 "integer"
:REV103 "integer"
:ALL901 "integer"
:ALE005 "integer"
:ILN905 "integer"
:RTR005 "integer"
:ALL760 "integer"
:RTL002 "integer"
:REV159 "integer"
:BAC028 "integer"
:ILN026 "integer"
:DelinquenciesOver60Days "integer"
:ALL001 "integer"
:BAC601 "integer"
:BAC903 "integer"
:FIN026 "integer"
:REV720 "integer"
:ILN081 "integer"
:REV752 "integer"
:RTL903 "integer"
:REV703 "integer"
:REV044 "integer"
:REV001 "integer"
:ALL501 "integer"
:REV124 "integer"
:RTR501 "integer"
:REV401 "integer"
:ILN126 "integer"
:REV119 "integer"
:BorrowerRate "double precision"
:REV107 "integer"
:BAC001 "integer"
:ILN118 "integer"
:EmploymentStatusDescription "varchar(120)"
:ALL051 "integer"
:RTR403 "integer"
:EstimatedLossRate "double precision"
:ILN064 "integer"
:BAC042 "integer"
:ALL123 "integer"
:RTR080 "integer"
:RTR751 "integer"
:BAC007 "integer"
:REP071 "integer"
:REV129 "integer"
:REV110 "integer"
:BAC804 "integer"
:ALE724 "integer"
:StatedMonthlyIncome "double precision"
:RTR075 "integer"
:ALE080 "integer"
:ALL724 "integer"
:FIN601 "integer"
:REP084 "integer"
:BorrowerCity "varchar(120)"
:ILN112 "integer"
:ALL905 "integer"
:InstallmentBalance "double precision"
:REV104 "integer"
:ILN703 "integer"
:ALL524 "integer"
:REV524 "integer"
:REV071 "integer"
:ILN071 "integer"
:ALL803 "integer"
:PriorProsperLoans61DPD "integer"
:ALL806 "integer"
:RTL901 "integer"
:RTR303 "integer"
:InvestmentTypeDescription "varchar(32)"
:LastUpdatedDate "timestamp with time zone"
:ILN901 "integer"
:LenderYield "double precision"
:RTR903 "integer"
:BankcardUtilization "double precision"
:BAC906 "integer"
:REV101 "integer"
:ListingAmountFunded "double precision"
:REV028 "integer"
:ALL121 "integer"
:PriorProsperLoans "integer"
:MonthlyDebt "double precision"
:REV502 "integer"
:BAC031 "integer"
:RTR071 "integer"
:DelinquenciesLast7Years "integer"
:ALL151 "integer"
:REP503 "integer"
:BNK001 "integer"
:ALL502 "integer"
:REV740 "integer"
:ILN078 "integer"
:ALE078 "integer"
:ALL022 "integer"
:REP501 "integer"
:ILN103 "integer"
:ALL122 "integer"
:RTL026 "integer"
:RTR074 "integer"
:ALE904 "integer"
:BAC589 "integer"
:ALL702 "integer"
:PriorProsperLoansBalanceOutstanding "double precision"
:LenderIndicator "integer"
:ILN740 "integer"
:CreditLinesLast7Years "integer"
:BAC035 "integer"
:ILN724 "integer"
:REV109 "integer"
:AUT720 "integer"
:REV085 "integer"
:ALL740 "integer"
:RTL084 "integer"
:ILN107 "integer"
:RTL076 "integer"
:ALL104 "integer"
:PriorProsperLoansActive "integer"
:ILN903 "integer"
:BAC045 "integer"
:ALE740 "integer"
:ALE901 "integer"
:ALL107 "integer"
:ALE081 "integer"
:ILN077 "integer"
:RTR901 "integer"
:RealEstateBalance "double precision"
:IncomeRange "integer"
:REP026 "integer"
:RTR904 "integer"
:RTR031 "integer"
:ALL080 "integer"
:ALL907 "integer"
:InquiriesLast6Months "integer"
:ALL105 "integer"
:RTL078 "integer"
:FIN001 "integer"
:REP905 "integer"
:ALL021 "integer"
:ALL804 "integer"
:ListingStartDate "timestamp with time zone"
:ALL134 "integer"
:ALL075 "integer"
:ListingPurpose "varchar(240)"
:REV550 "integer"
:REV130 "integer"
:REV078 "integer"
:ALE026 "integer"
:ALL155 "integer"
:ALL064 "integer"
:REV590 "integer"
:RTR584 "integer"
:ALL002 "integer"
:ALE007 "integer"
:ALE001 "integer"
:BAC044 "integer"
:ProsperRating "varchar(8)"
:REV023 "integer"
:TotalOpenRevolvingAccounts "integer"
:ALL801 "integer"
:SatisfactoryAccounts "integer"
:RevolvingAvailablePercent "integer"
:RTR905 "integer"
:ILN022 "integer"
:REF001 "integer"
:WasDelinquentDerog "integer"
:REV081 "integer"
:REV128 "integer"
:VerificationStage "integer"
:LFI801 "integer"
:ALL109 "integer"
:ILN101 "integer"
:PriorProsperLoansLatePaymentsOneMonthPlus "integer"
:ALL602 "integer"
:MonthsEmployed "integer"
:ILN301 "integer"
:RTL908 "integer"
:ALE906 "integer"
:ListingTitle "varchar(120)"
:ALL301 "integer"
:RTL501 "integer"
:BNK026 "integer"
:ILN302 "integer"
:RTR044 "integer"
:REV702 "integer"
:ListingCategory "integer"
:ALL790 "integer"
:ALL074 "integer"
:RTR023 "integer"
:ALL084 "integer"
:REV589 "integer"
:ALL720 "integer"
:REV901 "integer"
:ALL024 "integer"
:REV118 "integer"
:ALL156 "integer"
:FIN801 "integer"
:REV202 "integer"
:REV908 "integer"
:RevolvingBalance "double precision"
:REV403 "integer"
:AUT001 "integer"
:ILN914 "integer"
:ScoreXChange "varchar(10)"
:REV751 "integer"
:ALL807 "integer"
:ALE903 "integer"
:ALL112 "integer"
:BAC022 "integer"
:ALL505 "integer"
:RTL071 "integer"
:BAC026 "integer"
:REV074 "integer"
:ALE503 "integer"
:ILN006 "integer"
:BorrowerMetropolitanArea "varchar(120)"
:OldestTradeOpenDate "timestamp with time zone"
:REV504 "integer"
:RTL075 "integer"
:ILN601 "integer"
:TotalTradeItems "integer"
:ILN114 "integer"
:PriorProsperLoansCyclesBilled "integer"
:PublicRecordsLast12Months "integer"
:RTR752 "integer"
:REP075 "integer"
:ALL101 "integer"
:ILN904 "integer"
:BAC074 "integer"
:InvestmentTypeID "integer"
:ALL146 "integer"
:RTR078 "integer"
:ALE084 "integer"
:REP005 "integer"
:ILN076 "integer"
:BAC801 "integer"
:ILN113 "integer"
:BAC550 "integer"
:ILN503 "integer"
:RTR401 "integer"
:ALL903 "integer"
:ILN002 "integer"
:ALE720 "integer"
:ALL153 "integer"
:ALE071 "integer"
:BAC076 "integer"
:MemberKey "varchar(120)"
:ALL067 "integer"
:MinPriorProsperLoan "double precision"
:REV301 "integer"
:FirstRecordedCreditLine "timestamp with time zone"
:RTR007 "integer"
:AUT071 "integer"
:ILN001 "integer"
:ALL126 "integer"
:REV038 "integer"
:RTL001 "integer"
:RTR585 "integer"
:RTR001 "integer"
:REV067 "integer"
:REP908 "integer"
:ALL115 "integer"
:RTR002 "integer"
:ILN075 "integer"
:ILN119 "integer"
:REV724 "integer"
:REV904 "integer"
:ALL086 "integer"
:ILN115 "integer"
:BAC503 "integer"
:ILN124 "integer"
:ALE076 "integer"
:WholeLoanEndDate "timestamp with time zone"
:REV024 "integer"
:ScoreX "varchar(120)"
:GroupName "varchar(120)"
:REV302 "integer"
:ListingEndDate "timestamp with time zone"
:REV601 "integer"
:ALL108 "integer"
:ALL091 "integer"
:ALL129 "integer"
:REV005 "integer"
:REV113 "integer"
:RTR601 "integer"
:REV501 "integer"
:PriorProsperLoans31DPD "integer"
:RTR081 "integer"
:REV086 "integer"
:PriorProsperLoansPrincipalOutstanding "double precision"
:ALL052 "integer"})

(def numeric-fields
  (into {} (filter (comp #(or (= "integer" %)
                              (= "double precision" %)) val)
                   fields)))

(def character-fields
  (select-keys fields (remove (set (keys numeric-fields)) (keys fields))))
