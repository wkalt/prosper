(ns prosper.fields
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.java.jdbc.deprecated :as jdbcd]
            [clj-time.core :refer [now]]
            [cheshire.core :as json]
            [clojure.set :refer [difference]]
            [clj-time.coerce :refer [to-timestamp]]
            [clojure.tools.logging :as log]
            [prosper.query :as q]))

(def legacy-fields
  {:RTL005 "integer"
   :ChannelCode "varchar(120)"
   :ILN501 "integer"
   :CreditPullDate "timestamp with time zone"
   :ALL906 "integer"
   :REV905 "integer"
   :BorrowerListingDescription "varchar(240)"
   :ILN908 "integer"
   :REV077 "integer"
   :AmountRemaining "numeric"
   :ListingMonthlyPayment "numeric"
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
   :EstimatedReturn "numeric"
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
   :RealEstatePayment "numeric"
   :ALL106 "integer"
   :ALL103 "integer"
   :BAC081 "integer"
   :REV108 "integer"
   :BAC752 "integer"
   :TotalInquiries "integer"
   :BAC302 "integer"
   :LoanNumber "integer"
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
   :AmountParticipation "numeric"
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
   :BorrowerAPR "numeric"
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
   :PriorProsperLoansLateCycles "integer"
   :REV006 "integer"
   :MaxPriorProsperLoan "numeric"
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
   :PercentFunded "numeric"
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
   :AmountDelinquent "numeric"
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
   :FundingThreshold "numeric"
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
   :DTIwProsperLoan "numeric"
   :OpenCreditLines "integer"
   :REV026 "integer"
   :FIL023 "integer"
   :IsHomeowner "boolean"
   :DelinquenciesOver30Days "integer"
   :BAC303 "integer"
   :REV585 "integer"
   :ALL090 "integer"
   :ListingRequestAmount "numeric"
   :FIL001 "integer"
   :ILN106 "integer"
   :ALE801 "integer"
   :ILN084 "integer"
   :EffectiveYield "numeric"
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
   :BorrowerRate "numeric"
   :REV107 "integer"
   :BAC001 "integer"
   :ILN118 "integer"
   :EmploymentStatusDescription "varchar(120)"
   :ALL051 "integer"
   :RTR403 "integer"
   :EstimatedLossRate "numeric"
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
   :StatedMonthlyIncome "numeric"
   :RTR075 "integer"
   :ALE080 "integer"
   :ALL724 "integer"
   :FIN601 "integer"
   :REP084 "integer"
   :BorrowerCity "varchar(120)"
   :ILN112 "integer"
   :ALL905 "integer"
   :InstallmentBalance "numeric"
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
   :LenderYield "numeric"
   :RTR903 "integer"
   :BankcardUtilization "numeric"
   :BAC906 "integer"
   :REV101 "integer"
   :ListingAmountFunded "numeric"
   :REV028 "integer"
   :ALL121 "integer"
   :PriorProsperLoans "integer"
   :MonthlyDebt "numeric"
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
   :PriorProsperLoansBalanceOutstanding "numeric"
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
   :RealEstateBalance "numeric"
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
   :RevolvingBalance "numeric"
   :REV403 "integer"
   :AUT001 "integer"
   :ILN914 "integer"
   :ScoreXChange "varchar(25)"
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
   :OldestTradeOpenDate "varchar(20)"
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
   :MinPriorProsperLoan "numeric"
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
   :PriorProsperLoansPrincipalOutstanding "numeric"
   :ALL052 "integer"})

(def cb-fields
  [:RTL005
   :ILN501
   :ALL906
   :REV905
   :ILN908
   :REV077
   :ALL114
   :BAC159
   :ILN005
   :BAC908
   :ALL144
   :ALE905
   :REV201
   :ALL136
   :REV002
   :RTR590
   :ILN403
   :ALL076
   :ILN127
   :ALL601
   :ALL503
   :REP302
   :ALL805
   :REP906
   :BAC751
   :BAC904
   :REV106
   :ILN067
   :ALL007
   :BAC075
   :BAC078
   :BAC403
   :REP081
   :BAC901
   :ALE075
   :ALL904
   :RTR076
   :ALL006
   :ILN007
   :REV117
   :ILN906
   :ILN720
   :ALE501
   :ILN109
   :ALE804
   :RTL905
   :ALL082
   :ALL208
   :ALE601
   :ILN130
   :RTR077
   :CRU001
   :ILN105
   :BAC023
   :BAC077
   :REV075
   :RTL077
   :REV114
   :ALL010
   :REP002
   :ILN502
   :ALL118
   :ILN108
   :ILN504
   :ALL102
   :RTI026
   :REV584
   :ALL085
   :CAP026
   :ALL202
   :RTR908
   :ALL143
   :ILN074
   :ALE502
   :REV064
   :ALL023
   :ALL106
   :ALL103
   :BAC081
   :REV108
   :BAC752
   :BAC302
   :BAC084
   :ALE022
   :ILN804
   :ALL125
   :RTR906
   :ILN122
   :RTL502
   :REV084
   :ALL092
   :REV111
   :BAC502
   :ALL701
   :REV102
   :BAC002
   :REV022
   :FIL022
   :REV076
   :ILN023
   :REV116
   :ALL127
   :ILN117
   :REV125
   :ALL116
   :REP080
   :ALL201
   :REV503
   :ALL703
   :REV080
   :REV701
   :ILN201
   :REV903
   :GBL007
   :REV007
   :ILN125
   :ILN701
   :BAC501
   :ALL128
   :ALL152
   :REV006
   :ALL145
   :RTL080
   :ALL141
   :ALE908
   :REV906
   :RTR028
   :RTR026
   :REV122
   :BAC905
   :ALE077
   :ALL110
   :ALL207
   :ALL026
   :ALL062
   :ALL003
   :REP076
   :ALL113
   :ILN116
   :REP001
   :REP078
   :ALL078
   :REV105
   :ALL119
   :BAC080
   :ALL780
   :ALL111
   :ALL124
   :ILN080
   :CAP801
   :RTR589
   :REP074
   :REP903
   :RTR159
   :ILN110
   :RTR084
   :ILN086
   :REV127
   :ALL403
   :REP077
   :REP904
   :RTL074
   :ILN128
   :ALL005
   :ILN104
   :ILN129
   :ALE023
   :ALE002
   :ALL071
   :ILN111
   :HEQ001
   :RTR022
   :RTR035
   :RTL906
   :ALE074
   :ALL131
   :BAC584
   :REV126
   :ALL142
   :RTL503
   :ILN085
   :REV404
   :ALL130
   :REV112
   :BAC005
   :BAC071
   :REV115
   :BAC037
   :ALL081
   :BRR026
   :ILN102
   :RTL904
   :ALL077
   :BAC401
   :REV026
   :FIL023
   :BAC303
   :REV585
   :ALL090
   :FIL001
   :ILN106
   :ALE801
   :ILN084
   :ILN702
   :REP601
   :ALL117
   :ALL504
   :RTL081
   :REP901
   :ALE403
   :ILN801
   :REV103
   :ALL901
   :ALE005
   :ILN905
   :RTR005
   :ALL760
   :RTL002
   :REV159
   :BAC028
   :ILN026
   :ALL001
   :BAC601
   :BAC903
   :FIN026
   :REV720
   :ILN081
   :REV752
   :RTL903
   :REV703
   :REV044
   :REV001
   :ALL501
   :REV124
   :RTR501
   :REV401
   :ILN126
   :REV119
   :REV107
   :BAC001
   :ILN118
   :ALL051
   :RTR403
   :ILN064
   :BAC042
   :ALL123
   :RTR080
   :RTR751
   :BAC007
   :REP071
   :REV129
   :REV110
   :BAC804
   :ALE724
   :RTR075
   :ALE080
   :ALL724
   :FIN601
   :REP084
   :ILN112
   :ALL905
   :REV104
   :ILN703
   :ALL524
   :REV524
   :REV071
   :ILN071
   :ALL803
   :ALL806
   :RTL901
   :RTR303
   :ILN901
   :RTR903
   :BAC906
   :REV101
   :REV028
   :ALL121
   :REV502
   :BAC031
   :RTR071
   :ALL151
   :REP503
   :BNK001
   :ALL502
   :REV740
   :ILN078
   :ALE078
   :ALL022
   :REP501
   :ILN103
   :ALL122
   :RTL026
   :RTR074
   :ALE904
   :BAC589
   :ALL702
   :ILN740
   :BAC035
   :ILN724
   :REV109
   :AUT720
   :REV085
   :ALL740
   :RTL084
   :ILN107
   :RTL076
   :ALL104
   :ILN903
   :BAC045
   :ALE740
   :ALE901
   :ALL107
   :ALE081
   :ILN077
   :RTR901
   :REP026
   :RTR904
   :RTR031
   :ALL080
   :ALL907
   :ALL105
   :RTL078
   :FIN001
   :REP905
   :ALL021
   :ALL804
   :ALL134
   :ALL075
   :REV550
   :REV130
   :REV078
   :ALE026
   :ALL155
   :ALL064
   :REV590
   :RTR584
   :ALL002
   :ALE007
   :ALE001
   :BAC044
   :REV023
   :ALL801
   :RTR905
   :ILN022
   :REF001
   :REV081
   :REV128
   :LFI801
   :ALL109
   :ILN101
   :ALL602
   :ILN301
   :RTL908
   :ALE906
   :ALL301
   :RTL501
   :BNK026
   :ILN302
   :RTR044
   :REV702
   :ALL790
   :ALL074
   :RTR023
   :ALL084
   :REV589
   :ALL720
   :REV901
   :ALL024
   :REV118
   :ALL156
   :FIN801
   :REV202
   :REV908
   :REV403
   :AUT001
   :ILN914
   :REV751
   :ALL807
   :ALE903
   :ALL112
   :BAC022
   :ALL505
   :RTL071
   :BAC026
   :REV074
   :ALE503
   :ILN006
   :REV504
   :RTL075
   :ILN601
   :ILN114
   :RTR752
   :REP075
   :ALL101
   :ILN904
   :BAC074
   :ALL146
   :RTR078
   :ALE084
   :REP005
   :ILN076
   :BAC801
   :ILN113
   :BAC550
   :ILN503
   :RTR401
   :ALL903
   :ILN002
   :ALE720
   :ALL153
   :ALE071
   :BAC076
   :ALL067
   :REV301
   :RTR007
   :AUT071
   :ILN001
   :ALL126
   :REV038
   :RTL001
   :RTR585
   :RTR001
   :REV067
   :REP908
   :ALL115
   :RTR002
   :ILN075
   :ILN119
   :REV724
   :REV904
   :ALL086
   :ILN115
   :BAC503
   :ILN124
   :ALE076
   :REV024
   :REV302
   :REV601
   :ALL108
   :ALL091
   :ALL129
   :REV005
   :REV113
   :RTR601
   :REV501
   :RTR081
   :REV086
   :ALL052])


(def legacy->v1-conversions
  {:RTL005 "rtl005"
   :ChannelCode "channel_code"
   :ILN501 "iln501"
   :CreditPullDate "credit_pull_date"
   :ALL906 "all906"
   :REV905 "rev905"
   :listingnumber "listing_number"
   :BorrowerListingDescription "borrower_listing_description"
   :ILN908 "iln908"
   :REV077 "rev077"
   :AmountRemaining "amount_remaining"
   :ListingMonthlyPayment "listing_monthly_payment"
   :ALL114 "all114"
   :BAC159 "bac159"
   :ILN005 "iln005"
   :BAC908 "bac908"
   :ALL144 "all144"
   :ALE905 "ale905"
   :REV201 "rev201"
   :WholeLoanStartDate "whole_loan_start_date"
   :ALL136 "all136"
   :REV002 "rev002"
   :RTR590 "rtr590"
   :ILN403 "iln403"
   :ALL076 "all076"
   :ILN127 "iln127"
   :ALL601 "all601"
   :ALL503 "all503"
   :REP302 "rep302"
   :ALL805 "all805"
   :REP906 "rep906"
   :BAC751 "bac751"
   :BAC904 "bac904"
   :ListingTerm "listing_term"
   :REV106 "rev106"
   :ILN067 "iln067"
   :ALL007 "all007"
   :BAC075 "bac075"
   :BAC078 "bac078"
   :BAC403 "bac403"
   :REP081 "rep081"
   :BAC901 "bac901"
   :FICOScore "fico_score"
   :ALE075 "ale075"
   :ALL904 "all904"
   :RTR076 "rtr076"
   :ALL006 "all006"
   :ILN007 "iln007"
   :REV117 "rev117"
   :GroupIndicator "group_indicator"
   :ILN906 "iln906"
   :EstimatedReturn "estimated_return"
   :NowDelinquentDerog "now_delinquent_derog"
   :ILN720 "iln720"
   :ALE501 "ale501"
   :ILN109 "iln109"
   :ALE804 "ale804"
   :RTL905 "rtl905"
   :ALL082 "all082"
   :ALL208 "all208"
   :ALE601 "ale601"
   :ILN130 "iln130"
   :RTR077 "rtr077"
   :CRU001 "cru001"
   :ILN105 "iln105"
   :BAC023 "bac023"
   :BAC077 "bac077"
   :REV075 "rev075"
   :RTL077 "rtl077"
   :REV114 "rev114"
   :ALL010 "all010"
   :REP002 "rep002"
   :ILN502 "iln502"
   :ALL118 "all118"
   :ILN108 "iln108"
   :ILN504 "iln504"
   :ALL102 "all102"
   :RTI026 "rti026"
   :REV584 "rev584"
   :ALL085 "all085"
   :CAP026 "cap026"
   :ALL202 "all202"
   :RTR908 "rtr908"
   :ALL143 "all143"
   :ILN074 "iln074"
   :ListingStatusDescription "listing_status_reason"
   :ALE502 "ale502"
   :REV064 "rev064"
   :ALL023 "all023"
   :RealEstatePayment "real_estate_payment"
   :ALL106 "all106"
   :ALL103 "all103"
   :BAC081 "bac081"
   :REV108 "rev108"
   :BAC752 "bac752"
   :TotalInquiries "total_inquiries"
   :BAC302 "bac302"
   :LoanNumber "loan_number"
   :BAC084 "bac084"
   :IncomeVerifiable "income_verifiable"
   :ALE022 "ale022"
   :ILN804 "iln804"
   :ALL125 "all125"
   :RTR906 "rtr906"
   :ILN122 "iln122"
   :RTL502 "rtl502"
   :REV084 "rev084"
   :ALL092 "all092"
   :BorrowerState "borrower_state"
   :REV111 "rev111"
   :BAC502 "bac502"
   :ALL701 "all701"
   :REV102 "rev102"
   :BAC002 "bac002"
   :REV022 "rev022"
   :FIL022 "fil022"
   :REV076 "rev076"
   :AmountParticipation "amount_participation"
   :ILN023 "iln023"
   :REV116 "rev116"
   :ALL127 "all127"
   :ILN117 "iln117"
   :REV125 "rev125"
   :ALL116 "all116"
   :REP080 "rep080"
   :ALL201 "all201"
   :REV503 "rev503"
   :ALL703 "all703"
   :REV080 "rev080"
   :REV701 "rev701"
   :BorrowerAPR "borrower_apr"
   :ILN201 "iln201"
   :REV903 "rev903"
   :GBL007 "gbl007"
   :REV007 "rev007"
   :ILN125 "iln125"
   :ILN701 "iln701"
   :BAC501 "bac501"
   :ProsperScore "prosper_score"
   :ALL128 "all128"
   :ALL152 "all152"
   :PriorProsperLoansLateCycles "prior_prosper_loans_late_cycles"
   :REV006 "rev006"
   :MaxPriorProsperLoan "max_prior_prosper_loan"
   :ALL145 "all145"
   :RTL080 "rtl080"
   :ALL141 "all141"
   :PartialFundingIndicator "partial_funding_indicator"
   :ALE908 "ale908"
   :REV906 "rev906"
   :RTR028 "rtr028"
   :RTR026 "rtr026"
   :REV122 "rev122"
   :BAC905 "bac905"
   :ALE077 "ale077"
   :DelinquenciesOver90Days "delinquencies_over90_days"
   :ALL110 "all110"
   :ALL207 "all207"
   :ALL026 "all026"
   :ALL062 "all062"
   :ALL003 "all003"
   :REP076 "rep076"
   :ALL113 "all113"
   :ILN116 "iln116"
   :REP001 "rep001"
   :REP078 "rep078"
   :LoanOriginationDate "loan_origination_date"
   :ALL078 "all078"
   :PercentFunded "percent_funded"
   :REV105 "rev105"
   :ALL119 "all119"
   :BAC080 "bac080"
   :ALL780 "all780"
   :ALL111 "all111"
   :ALL124 "all124"
   :ILN080 "iln080"
   :CAP801 "cap801"
   :RTR589 "rtr589"
   :REP074 "rep074"
   :REP903 "rep903"
   :RTR159 "rtr159"
   :ILN110 "iln110"
   :RTR084 "rtr084"
   :ILN086 "iln086"
   :AmountDelinquent "amount_delinquent"
   :PublicRecordsLast10Years "public_records_last10_years"
   :REV127 "rev127"
   :ALL403 "all403"
   :REP077 "rep077"
   :REP904 "rep904"
   :RTL074 "rtl074"
   :ILN128 "iln128"
   :ALL005 "all005"
   :CurrentCreditLines "current_credit_lines"
   :ILN104 "iln104"
   :ILN129 "iln129"
   :PriorProsperLoansPrincipalBorrowed "prior_prosper_loans_principal_borrowed"
   :ALE023 "ale023"
   :PriorProsperLoanEarliestPayOff "prior_prosper_loan_earliest_pay_off"
   :ALE002 "ale002"
   :ALL071 "all071"
   :ILN111 "iln111"
   :HEQ001 "heq001"
   :RTR022 "rtr022"
   :RTR035 "rtr035"
   :RTL906 "rtl906"
   :ALE074 "ale074"
   :ALL131 "all131"
   :BAC584 "bac584"
   :REV126 "rev126"
   :ALL142 "all142"
   :RTL503 "rtl503"
   :ILN085 "iln085"
   :REV404 "rev404"
   :ALL130 "all130"
   :REV112 "rev112"
   :ListingStatus "listing_status"
   :BAC005 "bac005"
   :CurrentDelinquencies "current_delinquencies"
   :BAC071 "bac071"
   :REV115 "rev115"
   :FundingThreshold "funding_threshold"
   :BAC037 "bac037"
   :ALL081 "all081"
   :ListingCreationDate "listing_creation_date"
   :BRR026 "brr026"
   :ILN102 "iln102"
   :RTL904 "rtl904"
   :ALL077 "all077"
   :BAC401 "bac401"
   :IncomeRangeDescription "income_range_description"
   :PriorProsperLoansOnTimePayments "prior_prosper_loans_ontime_payments"
   :Occupation "occupation"
   :DTIwProsperLoan "dt_iwprosper_loan"
   :OpenCreditLines "open_credit_lines"
   :REV026 "rev026"
   :FIL023 "fil023"
   :IsHomeowner "is_homeowner"
   :DelinquenciesOver30Days "delinquencies_over30_days"
   :BAC303 "bac303"
   :REV585 "rev585"
   :ALL090 "all090"
   :ListingRequestAmount "listing_amount"
   :FIL001 "fil001"
   :ILN106 "iln106"
   :ALE801 "ale801"
   :ILN084 "iln084"
   :EffectiveYield "effective_yield"
   :ILN702 "iln702"
   :REP601 "rep601"
   :ALL117 "all117"
   :ALL504 "all504"
   :RTL081 "rtl081"
   :REP901 "rep901"
   :ALE403 "ale403"
   :ILN801 "iln801"
   :REV103 "rev103"
   :ALL901 "all901"
   :ALE005 "ale005"
   :ILN905 "iln905"
   :RTR005 "rtr005"
   :ALL760 "all760"
   :RTL002 "rtl002"
   :REV159 "rev159"
   :BAC028 "bac028"
   :ILN026 "iln026"
   :DelinquenciesOver60Days "delinquencies_over60_days"
   :ALL001 "all001"
   :BAC601 "bac601"
   :BAC903 "bac903"
   :FIN026 "fin026"
   :REV720 "rev720"
   :ILN081 "iln081"
   :REV752 "rev752"
   :RTL903 "rtl903"
   :REV703 "rev703"
   :REV044 "rev044"
   :REV001 "rev001"
   :ALL501 "all501"
   :REV124 "rev124"
   :RTR501 "rtr501"
   :REV401 "rev401"
   :ILN126 "iln126"
   :REV119 "rev119"
   :BorrowerRate "borrower_rate"
   :REV107 "rev107"
   :BAC001 "bac001"
   :ILN118 "iln118"
   :EmploymentStatusDescription "employment_status_description"
   :ALL051 "all051"
   :RTR403 "rtr403"
   :EstimatedLossRate "estimated_loss_rate"
   :ILN064 "iln064"
   :BAC042 "bac042"
   :ALL123 "all123"
   :RTR080 "rtr080"
   :RTR751 "rtr751"
   :BAC007 "bac007"
   :REP071 "rep071"
   :REV129 "rev129"
   :REV110 "rev110"
   :BAC804 "bac804"
   :ALE724 "ale724"
   :StatedMonthlyIncome "stated_monthly_income"
   :RTR075 "rtr075"
   :ALE080 "ale080"
   :ALL724 "all724"
   :FIN601 "fin601"
   :REP084 "rep084"
   :BorrowerCity "borrower_city"
   :ILN112 "iln112"
   :ALL905 "all905"
   :InstallmentBalance "installment_balance"
   :REV104 "rev104"
   :ILN703 "iln703"
   :ALL524 "all524"
   :REV524 "rev524"
   :REV071 "rev071"
   :ILN071 "iln071"
   :ALL803 "all803"
   :PriorProsperLoans61DPD "prior_prosper_loans61dpd"
   :ALL806 "all806"
   :RTL901 "rtl901"
   :RTR303 "rtr303"
   :InvestmentTypeDescription "investment_type_description"
   :LastUpdatedDate "last_updated_date"
   :ILN901 "iln901"
   :LenderYield "lender_yield"
   :RTR903 "rtr903"
   :BankcardUtilization "bankcard_utilization"
   :BAC906 "bac906"
   :REV101 "rev101"
   :ListingAmountFunded "amount_funded"
   :REV028 "rev028"
   :ALL121 "all121"
   :PriorProsperLoans "prior_prosper_loans"
   :MonthlyDebt "monthly_debt"
   :REV502 "rev502"
   :BAC031 "bac031"
   :RTR071 "rtr071"
   :DelinquenciesLast7Years "delinquencies_last7_years"
   :ALL151 "all151"
   :REP503 "rep503"
   :BNK001 "bnk001"
   :ALL502 "all502"
   :REV740 "rev740"
   :ILN078 "iln078"
   :ALE078 "ale078"
   :ALL022 "all022"
   :REP501 "rep501"
   :ILN103 "iln103"
   :ALL122 "all122"
   :RTL026 "rtl026"
   :RTR074 "rtr074"
   :ALE904 "ale904"
   :BAC589 "bac589"
   :ALL702 "all702"
   :PriorProsperLoansBalanceOutstanding "prior_prosper_loans_balance_outstanding"
   :LenderIndicator "lender_indicator"
   :ILN740 "iln740"
   :CreditLinesLast7Years "credit_lines_last7_years"
   :BAC035 "bac035"
   :ILN724 "iln724"
   :REV109 "rev109"
   :AUT720 "aut720"
   :REV085 "rev085"
   :ALL740 "all740"
   :RTL084 "rtl084"
   :ILN107 "iln107"
   :RTL076 "rtl076"
   :ALL104 "all104"
   :PriorProsperLoansActive "prior_prosper_loans_active"
   :ILN903 "iln903"
   :BAC045 "bac045"
   :ALE740 "ale740"
   :ALE901 "ale901"
   :ALL107 "all107"
   :ALE081 "ale081"
   :ILN077 "iln077"
   :RTR901 "rtr901"
   :RealEstateBalance "real_estate_balance"
   :IncomeRange "income_range"
   :REP026 "rep026"
   :RTR904 "rtr904"
   :RTR031 "rtr031"
   :ALL080 "all080"
   :ALL907 "all907"
   :InquiriesLast6Months "inquiries_last6_months"
   :ALL105 "all105"
   :RTL078 "rtl078"
   :FIN001 "fin001"
   :REP905 "rep905"
   :ALL021 "all021"
   :ALL804 "all804"
   :ListingStartDate "listing_start_date"
   :ALL134 "all134"
   :ALL075 "all075"
   :ListingPurpose "listing_purpose"
   :REV550 "rev550"
   :REV130 "rev130"
   :REV078 "rev078"
   :ALE026 "ale026"
   :ALL155 "all155"
   :ALL064 "all064"
   :REV590 "rev590"
   :RTR584 "rtr584"
   :ALL002 "all002"
   :ALE007 "ale007"
   :ALE001 "ale001"
   :BAC044 "bac044"
   :ProsperRating "prosper_rating"
   :REV023 "rev023"
   :TotalOpenRevolvingAccounts "total_open_revolving_accounts"
   :ALL801 "all801"
   :SatisfactoryAccounts "satisfactory_accounts"
   :RevolvingAvailablePercent "revolving_available_percent"
   :RTR905 "rtr905"
   :ILN022 "iln022"
   :REF001 "ref001"
   :WasDelinquentDerog "was_delinquent_derog"
   :REV081 "rev081"
   :REV128 "rev128"
   :VerificationStage "verification_stage"
   :LFI801 "lfi801"
   :ALL109 "all109"
   :ILN101 "iln101"
   :PriorProsperLoansLatePaymentsOneMonthPlus "prior_prosper_loans_late_payments_one_month_plus"
   :ALL602 "all602"
   :MonthsEmployed "months_employed"
   :ILN301 "iln301"
   :RTL908 "rtl908"
   :ALE906 "ale906"
   :ListingTitle "listing_title"
   :ALL301 "all301"
   :RTL501 "rtl501"
   :BNK026 "bnk026"
   :ILN302 "iln302"
   :RTR044 "rtr044"
   :REV702 "rev702"
   :ListingCategory "listing_category_id"
   :ALL790 "all790"
   :ALL074 "all074"
   :RTR023 "rtr023"
   :ALL084 "all084"
   :REV589 "rev589"
   :ALL720 "all720"
   :REV901 "rev901"
   :ALL024 "all024"
   :REV118 "rev118"
   :ALL156 "all156"
   :FIN801 "fin801"
   :REV202 "rev202"
   :REV908 "rev908"
   :RevolvingBalance "revolving_balance"
   :REV403 "rev403"
   :AUT001 "aut001"
   :ILN914 "iln914"
   :ScoreXChange "scorex_change"
   :REV751 "rev751"
   :ALL807 "all807"
   :ALE903 "ale903"
   :ALL112 "all112"
   :BAC022 "bac022"
   :ALL505 "all505"
   :RTL071 "rtl071"
   :BAC026 "bac026"
   :REV074 "rev074"
   :ALE503 "ale503"
   :ILN006 "iln006"
   :BorrowerMetropolitanArea "borrower_metropolitan_area"
   :OldestTradeOpenDate "oldest_trade_open_date"
   :REV504 "rev504"
   :RTL075 "rtl075"
   :ILN601 "iln601"
   :TotalTradeItems "total_trade_items"
   :ILN114 "iln114"
   :PriorProsperLoansCyclesBilled "prior_prosper_loans_cycles_billed"
   :PublicRecordsLast12Months "public_records_last12_months"
   :RTR752 "rtr752"
   :REP075 "rep075"
   :ALL101 "all101"
   :ILN904 "iln904"
   :BAC074 "bac074"
   :InvestmentTypeID "investment_typeid"
   :ALL146 "all146"
   :RTR078 "rtr078"
   :ALE084 "ale084"
   :REP005 "rep005"
   :ILN076 "iln076"
   :BAC801 "bac801"
   :ILN113 "iln113"
   :BAC550 "bac550"
   :ILN503 "iln503"
   :RTR401 "rtr401"
   :ALL903 "all903"
   :ILN002 "iln002"
   :ALE720 "ale720"
   :ALL153 "all153"
   :ALE071 "ale071"
   :BAC076 "bac076"
   :MemberKey "member_key"
   :ALL067 "all067"
   :MinPriorProsperLoan "min_prior_prosper_loan"
   :REV301 "rev301"
   :FirstRecordedCreditLine "first_recorded_credit_line"
   :RTR007 "rtr007"
   :AUT071 "aut071"
   :ILN001 "iln001"
   :ALL126 "all126"
   :REV038 "rev038"
   :RTL001 "rtl001"
   :RTR585 "rtr585"
   :RTR001 "rtr001"
   :REV067 "rev067"
   :REP908 "rep908"
   :ALL115 "all115"
   :RTR002 "rtr002"
   :ILN075 "iln075"
   :ILN119 "iln119"
   :REV724 "rev724"
   :REV904 "rev904"
   :ALL086 "all086"
   :ILN115 "iln115"
   :BAC503 "bac503"
   :ILN124 "iln124"
   :ALE076 "ale076"
   :WholeLoanEndDate "whole_loan_end_date"
   :REV024 "rev024"
   :ScoreX "scorex"
   :GroupName "group_name"
   :REV302 "rev302"
   :ListingEndDate "listing_end_date"
   :REV601 "rev601"
   :ALL108 "all108"
   :ALL091 "all091"
   :ALL129 "all129"
   :REV005 "rev005"
   :REV113 "rev113"
   :RTR601 "rtr601"
   :REV501 "rev501"
   :PriorProsperLoans31DPD "prior_prosper_loans31dpd"
   :RTR081 "rtr081"
   :REV086 "rev086"
   :PriorProsperLoansPrincipalOutstanding "prior_prosper_loans_principal_outstanding"
   :ALL052 "all052"})

(def v1-fields
  (reduce #(assoc %1 (keyword (second %2)) ((first %2) legacy-fields)) {} legacy->v1-conversions))

(def date-fields
  (into {} (filter (comp #(= "timestamp with time zone" %) val)
                   v1-fields)))
