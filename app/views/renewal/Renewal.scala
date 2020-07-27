/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package views.renewal

import javax.inject.Inject
import views.html.renewal._

class Renewal @Inject()(
                       amls_turnover: amls_turnover,
                       amp_turnover: amp_turnover,
                       business_turnover: business_turnover,
                       cash_payments_customers_not_met: cash_payments_customers_not_met,
                       ce_transactions_in_last_12_months: ce_transactions_in_last_12_months,
                       customers_outside_uk: customers_outside_uk,
                       customers_outside_uk_isUK: customers_outside_uk_isUK,
                       fx_transaction_in_last_12_months: fx_transaction_in_last_12_months,
                       how_cash_payments_received: how_cash_payments_received,
                       involved_in_other: involved_in_other,
                       money_sources: money_sources,
                       most_transactions: most_transactions,
                       percentage: percentage,
                       renewal_progress: renewal_progress,
                       send_largest_amounts_of_money: send_largest_amounts_of_money,
                       send_money_to_other_country: send_money_to_other_country,
                       summary: summary,
                       total_throughput: total_throughput,
                       transactions_in_last_12_months: transactions_in_last_12_months,
                       uses_foreign_currencies: uses_foreign_currencies,
                       what_you_need: what_you_need,
                       which_currencies: which_currencies
                       ) {

}
