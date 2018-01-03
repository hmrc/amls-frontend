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

package connectors

import uk.gov.hmrc.domain.{CtUtr, SaUtr, Org}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{CtAccount, SaAccount, OrgAccount}

object ConnectorHelper {

  protected[connectors] def accountTypeAndId(implicit ac: AuthContext): (String, String) = {
    val accounts = ac.principal.accounts

    accounts.org match {
      case Some(OrgAccount(_, Org(ref))) =>("org", ref)
      case _ => accounts.sa match {
        case Some(SaAccount(_, SaUtr(ref))) => ("sa", ref)
        case _ =>  accounts.ct match {
          case Some(CtAccount(_, CtUtr(ref))) => ("ct", ref)
          case _ =>throw new IllegalArgumentException("authcontext does not contain any of the expected account types")
        }
      }
    }
  }
}

