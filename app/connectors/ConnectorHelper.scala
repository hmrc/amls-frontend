/*
 * Copyright 2019 HM Revenue & Customs
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

import uk.gov.hmrc.auth.core.{AffinityGroup, Enrolments}
import uk.gov.hmrc.domain.{CtUtr, Org, SaUtr}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{CtAccount, OrgAccount, SaAccount}
import utils.UrlSafe

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

  protected[connectors] def accountTypeAndIdFromEnrolments(affinityGroup: AffinityGroup,
                                                           enrolments: Enrolments,
                                                           credId: String): (String, String) = {

    /*
     * Set the `accountType` to `"org"` if `affinityGroup = "Organisation"` (which you get through retrievals)
     * Set the `accountId` as a hash of the CredId. Its possible to get the `credId` through retrievals
     */

    /*
     * For an affinity group other than Org;
     * Retrieve the enrolments through retrievals.
     * If one of them is `"IR-SA"`, you can set `accountType` to `"sa"` and `accountId` to the `value` for `key` `"UTR"`
     * If one of them is `"IR-CT"`, you can set `accountType` to `"ct"` and `accountId` to the `value` for `key` `"UTR"`
     */

    affinityGroup match {
      case AffinityGroup.Organisation => ("org", UrlSafe.hash(credId))
      case _ =>

        val sa = for {
          enrolment <- enrolments.getEnrolment("IR-SA")
          utr       <- enrolment.getIdentifier("UTR")
        } yield "sa" -> utr.value

        val ct = for {
          enrolment <- enrolments.getEnrolment("IR-CT")
          utr       <- enrolment.getIdentifier("UTR")
        } yield "ct" -> utr.value

        (sa orElse ct).getOrElse(throw new IllegalArgumentException("auth does not contain any of the expected account types"))
    }
  }
}

