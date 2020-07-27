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

package views.declaration

import javax.inject.Inject
import views.html.declaration._

class Declaration @Inject()(
                           add_person: add_person,
                           declare: declare,
                           register_partners: register_partners,
                           register_responsible_person: register_responsible_person,
                           renew_registration: renew_registration,
                           select_business_nominated_officer: select_business_nominated_officer,
                           who_is_registering_common: who_is_registering_common,
                           who_is_registering_this_registration: who_is_registering_this_registration,
                           who_is_registering_this_renewal: who_is_registering_this_renewal,
                           who_is_registering_this_update: who_is_registering_this_update
                           ) {

}
