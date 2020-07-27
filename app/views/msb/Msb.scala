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

package views.msb

import javax.inject.Inject
import views.html.msb._

class Msb @Inject()(
                   branches_or_agents: branches_or_agents,
                   branches_or_agents_which_countries: branches_or_agents_which_countries,
                   business_use_an_ipsp: business_use_an_ipsp,
                   ce_transaction_in_next_12_months: ce_transaction_in_next_12_months,
                   expected_throughput: expected_throughput,
                   funds_transfer: funds_transfer,
                   fx_transaction_in_next_12_months: fx_transaction_in_next_12_months,
                   identify_linked_transactions: identify_linked_transactions,
                   money_sources: money_sources,
                   most_transactions: most_transactions,
                   send_largest_amounts_of_money: send_largest_amounts_of_money,
                   send_money_to_other_country: send_money_to_other_country,
                   summary: summary,
                   transactions_in_next_12_months: transactions_in_next_12_months,
                   uses_foreign_currencies: uses_foreign_currencies,
                   what_you_need: what_you_need,
                   which_currencies: which_currencies
                   ) {

}
