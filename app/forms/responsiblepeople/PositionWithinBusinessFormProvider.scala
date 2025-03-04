/*
 * Copyright 2024 HM Revenue & Customs
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

package forms.responsiblepeople

import forms.mappings.Mappings
import models.responsiblepeople.{Other, PositionWithinBusiness}
import play.api.data.Form
import play.api.data.Forms.{mapping, seq}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class PositionWithinBusinessFormProvider @Inject() () extends Mappings {

  val length = 255 // TODO surely this is too long???

  private val checkboxError = "error.required.positionWithinBusiness"

  def apply(): Form[Set[PositionWithinBusiness]] = Form[Set[PositionWithinBusiness]](
    mapping(
      "positions"     -> seq(
        enumerable[PositionWithinBusiness](checkboxError, checkboxError)(PositionWithinBusiness.enumerable)
      ).verifying(nonEmptySeq(checkboxError)),
      "otherPosition" -> mandatoryIf(
        _.values.asJavaCollection.contains(Other("").toString),
        text("responsiblepeople.position_within_business.other_position.othermissing").verifying(
          firstError(
            maxLength(length, "error.invalid.rp.position_within_business.other_position.maxlength.255"),
            regexp(basicPunctuationRegex, "error.invalid.rp.position_within_business.other_position")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(
    positionsWithinBusiness: Seq[PositionWithinBusiness],
    maybeOtherPosition: Option[String]
  ): Set[PositionWithinBusiness] = (positionsWithinBusiness, maybeOtherPosition) match {
    case (positions, Some(name)) if positions.contains(Other("")) =>
      positions.map(x => if (x == Other("")) Other(name) else x).toSet
    case (positions, Some(n)) if !positions.contains(Other(""))   =>
      throw new IllegalArgumentException("Cannot have name without digital software")
    case (positions, None) if positions.contains(Other(""))       =>
      throw new IllegalArgumentException("Cannot have digital software without name")
    case (positions, None) if !positions.contains(Other(""))      => positions.toSet
  }

  private def unapply(obj: Set[PositionWithinBusiness]): Option[(Seq[PositionWithinBusiness], Option[String])] = {

    val positions = obj.toSeq.map {
      case Other(_) => Other("")
      case pos      => pos
    }

    val otherPosition = obj.find(_.isInstanceOf[Other]).flatMap {
      case Other(value) => Some(value)
      case _            => None
    }

    Some((positions, otherPosition))
  }

}
