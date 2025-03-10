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

package forms.businessactivities

import forms.mappings.Mappings
import models.businessactivities.TransactionTypes.{DigitalOther, DigitalSoftware, DigitalSpreadsheet, Paper}
import models.businessactivities.{TransactionType, TransactionTypes}
import play.api.data.Form
import play.api.data.Forms.{mapping, seq}
import uk.gov.voa.play.form.ConditionalMappings._

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class TransactionTypesFormProvider @Inject() () extends Mappings {

  val length                                                                                             = 40
  def apply(): Form[TransactionTypes]                                                                    = Form[TransactionTypes](
    mapping(
      "types"    -> seq(
        enumerable[TransactionType]("error.required.ba.atleast.one.transaction.record")(TransactionTypes.enumerable)
      )
        .verifying(
          nonEmptySeq("error.required.ba.atleast.one.transaction.record")
        ),
      "software" -> mandatoryIf(
        _.values.asJavaCollection.contains(DigitalOther.toString),
        text("error.required.ba.software.package.name").verifying(
          firstError(
            maxLength(length, "error.max.length.ba.software.package.name"),
            regexp(basicPunctuationRegex, "error.invalid.characters.ba.software.package.name")
          )
        )
      )
    )(apply)(unapply)
  )
  private def apply(transactionTypes: Seq[TransactionType], maybeName: Option[String]): TransactionTypes =
    (transactionTypes, maybeName) match {
      case (tt, Some(n)) if tt.contains(DigitalOther)  =>
        val modifiedTransactions = tt.map(t => if (t == DigitalOther) DigitalSoftware(n) else t)
        TransactionTypes(modifiedTransactions.toSet)
      case (tt, Some(n)) if !tt.contains(DigitalOther) =>
        throw new IllegalArgumentException("Cannot have name without digital software")
      case (tt, None) if tt.contains(DigitalOther)     =>
        throw new IllegalArgumentException("Cannot have digital software without name")
      case (tt, None) if !tt.contains(DigitalOther)    => TransactionTypes(tt.toSet)
    }

  private def unapply(obj: TransactionTypes): Option[(Seq[TransactionType], Option[String])] = {
    val objTypes = obj.types.toSeq.map {
      case Paper              => Paper
      case DigitalSpreadsheet => DigitalSpreadsheet
      case DigitalSoftware(_) => DigitalOther
    }

    val maybeName = obj.types.find(_.isInstanceOf[DigitalSoftware]).flatMap {
      case DigitalSoftware(name) => Some(name)
      case _                     => None
    }

    Some((objTypes, maybeName))
  }

}
