/*
 * Copyright 2017 HM Revenue & Customs
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

import org.scalatest.mock.MockitoSugar
import  utils.GenericTestHelper
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{OrgAccount, SaAccount, CtAccount, Accounts}

class AmlsRegimeSpec extends GenericTestHelper with MockitoSugar {

  "AmlsRegimeSpec" should {

    "Correctly calculate isAuthorised" in {

      val ct = Accounts(ct = Some(mock[CtAccount]))
      val org = Accounts(org = Some(mock[OrgAccount]))
      val sa = Accounts(sa = Some(mock[SaAccount]))

      AmlsRegime.isAuthorised(ct) must be (true)
      AmlsRegime.isAuthorised(org) must be (true)
      AmlsRegime.isAuthorised(sa) must be (true)
      AmlsRegime.isAuthorised(Accounts()) must be (false)

    }

    "return the correct route for unauthorised page" in {
      AmlsRegime.unauthorisedLandingPage must be (Some(controllers.routes.AmlsController.unauthorised().url))
    }


  }

}
