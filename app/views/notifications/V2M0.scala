/*
 * Copyright 2022 HM Revenue & Customs
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

package views.notifications

import javax.inject.Inject
import views.html.notifications.v2m0._

class V2M0 @Inject()(
                      val message_details: message_details,
                      val minded_to_reject: minded_to_reject,
                      val minded_to_revoke: minded_to_revoke,
                      val no_longer_minded_to_reject: no_longer_minded_to_reject,
                      val no_longer_minded_to_revoke :no_longer_minded_to_revoke,
                      val rejection_reasons: rejection_reasons,
                      val revocation_reasons: revocation_reasons
                    )
