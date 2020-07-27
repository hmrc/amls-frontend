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

package views.confirmation

import javax.inject.Inject
import views.html.confirmation._

class Confirmation @Inject()(
                            confirm_amendvariation: confirm_amendvariation,
                            confirm_renewal: confirm_renewal,
                            confirmation_bacs: confirmation_bacs,
                            confirmation_bacs_transitional_renewal: confirmation_bacs_transitional_renewal,
                            confirmation_new: confirmation_new,
                            confirmation_no_fee: confirmation_no_fee,
                            footer: footer,
                            payment_confirmation: payment_confirmation,
                            payment_confirmation_amendvariation: payment_confirmation_amendvariation,
                            payment_confirmation_renewal: payment_confirmation_renewal,
                            payment_confirmation_transitional_renewal: payment_confirmation_transitional_renewal,
                            payment_failure: payment_failure
                            ) {

}
