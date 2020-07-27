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

package views.tradingpremises

import javax.inject.Inject
import views.html.tradingpremises._

class TradingPremises @Inject()(
                                 activity_start_date: activity_start_date,
                                 agent_company_details: agent_company_details,
                                 agent_company_name: agent_company_name,
                                 agent_name: agent_name,
                                 agent_partnership: agent_partnership,
                                 business_structure: business_structure,
                                 confirm_address: confirm_address,
                                 is_residential: is_residential,
                                 msb_services: msb_services,
                                 registering_agent_premises: registering_agent_premises,
                                 remove_agent_premises_reasons: remove_agent_premises_reasons,
                                 remove_trading_premises: remove_trading_premises,
                                 summary: summary,
                                 what_does_your_business_do: what_does_your_business_do,
                                 what_you_need: what_you_need,
                                 where_are_trading_premises: where_are_trading_premises,
                                 your_trading_premises: your_trading_premises
                               ) {

}
