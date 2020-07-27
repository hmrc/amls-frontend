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

package views.hvd

import javax.inject.Inject
import views.html.hvd._

class Hvd @Inject()(
                   cash_payment: cash_payment,
                   cash_payment_first_Date:cash_payment_first_date,
                   excise_goods: excise_goods,
                   expect_to_receive: expect_to_receive,
                   how_will_you_sell_goods: how_will_you_sell_goods,
                   linked_cash_payments: linked_cash_payments,
                   percentage: percentage,
                   products: products,
                   receiving: receiving,
                   summary: summary,
                   what_you_need: what_you_need
                   ) {

}
