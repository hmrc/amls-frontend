/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers.auth

import uk.gov.hmrc.play.frontend.auth.connectors.domain.Accounts
import uk.gov.hmrc.play.frontend.auth.{AuthenticationProvider, TaxRegime}

object AmlsRegime extends TaxRegime {

  // TODO: must include amls regime
  override def isAuthorised(accounts: Accounts): Boolean = {
    accounts.ct.isDefined || accounts.org.isDefined || accounts.sa.isDefined
  }

  override def authenticationType: AuthenticationProvider = AmlsGovernmentGateway

  override def unauthorisedLandingPage: Option[String] = Some(controllers.routes.AmlsController.unauthorised().url)
}
