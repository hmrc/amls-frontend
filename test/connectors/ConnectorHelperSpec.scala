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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.{CtUtr, SaUtr}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{Principal, LoggedInUser, AuthContext}

class ConnectorHelperSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  "return sa account type and reference" when {
    "Logged in user is sa account type" in {

      implicit val saAcct = AuthContext(
        LoggedInUser(
          "UserName",
          None,
          None,
          None,
          CredentialStrength.Weak,
          ConfidenceLevel.L50, ""),
        Principal(
          None,
          Accounts(sa = Some(SaAccount("Link", SaUtr("saRef"))))),
        None,
        None,
        None, None)
      ConnectorHelper.accountTypeAndId(saAcct) must be(("sa", "saRef"))
    }
  }

  "return ct account type and reference" when {
    "Logged in user is ct account type" in {

      implicit val ctAcct = AuthContext(
        LoggedInUser(
          "UserName",
          None,
          None,
          None,
          CredentialStrength.Weak,
          ConfidenceLevel.L50, ""),
        Principal(
          None,
          Accounts(ct = Some(CtAccount("Link", CtUtr("ctRef"))))),
        None,
        None,
        None, None)
      ConnectorHelper.accountTypeAndId(ctAcct) must be(("ct", "ctRef"))
    }
  }

  "fail on not finding correct accountType" in {

    implicit val ctAcct = AuthContext(
      LoggedInUser(
        "UserName",
        None,
        None,
        None,
        CredentialStrength.Weak,
        ConfidenceLevel.L50, ""),
      Principal(
        None,
        Accounts(ct = None)),
      None,
      None,
      None, None)
    an[IllegalArgumentException] should be thrownBy ConnectorHelper.accountTypeAndId(ctAcct)
  }
}
