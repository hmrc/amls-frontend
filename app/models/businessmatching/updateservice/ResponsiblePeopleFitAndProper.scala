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

package models.businessmatching.updateservice

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import utils.TraversableValidators.minLengthR

case class ResponsiblePeopleFitAndProper(index: Set[Int])

object ResponsiblePeopleFitAndProper {

  import jto.validation.forms.Rules._
  import utils.MappingUtils.Implicits._

  implicit def formReads: Rule[UrlFormEncoded, ResponsiblePeopleFitAndProper] = From[UrlFormEncoded] { __ =>
    (__ \ "responsiblePeople")
      .read(minLengthR[Set[Int]](1).withMessage("error.businessmatching.updateservice.responsiblepeople"))
      .flatMap(ResponsiblePeopleFitAndProper.apply)
  }

  implicit def formWrites(implicit w: Write[String, String]) = Write[ResponsiblePeopleFitAndProper, UrlFormEncoded] { data =>
    Map("responsiblePeople[]" -> data.index.toSeq.map(x => w.writes(x.toString)))
  }

}