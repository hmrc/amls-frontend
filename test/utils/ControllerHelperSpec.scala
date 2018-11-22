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

package utils

import models.responsiblepeople.ResponsiblePerson
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class ControllerHelperSpec  extends PlaySpec with MockitoSugar {

  def createResponsiblePersonSeq: Option[Seq[ResponsiblePerson]] = {
    None
  }

  "ControllerHelper" must {
    "hasIncompleteResponsiblePerson" must {
      "return false" when {
        "no responsiblePerson is supplied" in {
          val rp: Option[Seq[ResponsiblePerson]] = createResponsiblePersonSeq
          val hasIncomplete = ControllerHelper.hasIncompleteResponsiblePerson(rp)

          hasIncomplete mustEqual false
        }
      }
    }
  }
}
