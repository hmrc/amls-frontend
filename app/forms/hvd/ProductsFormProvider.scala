/*
 * Copyright 2023 HM Revenue & Customs
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

package forms.hvd

import forms.mappings.Mappings
import models.hvd.Products.Other
import models.hvd.{ItemType, Products}
import play.api.data.{Form, Forms}
import play.api.data.Forms.{mapping, seq}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

import javax.inject.Inject
import scala.collection.convert.ImplicitConversions.`collection asJava`

class ProductsFormProvider @Inject()() extends Mappings {

  private val checkboxError = "error.required.hvd.business.sell.atleast"

  val length = 255 //TODO This looks way too long
  def apply(): Form[Products] = Form[Products](
    mapping(
      "products" -> seq(enumerable[ItemType](checkboxError, checkboxError)(Products.enumerable))
        .verifying(nonEmptySeq(checkboxError)),
      "otherDetails" -> mandatoryIf(
        _.values.contains(Other("").toString),
        text("error.required.hvd.business.sell.other.details").verifying(
          firstError(
            maxLength(length, "error.invalid.hvd.business.sell.other.details"),
            regexp(basicPunctuationRegex, "error.invalid.hvd.business.sell.other.format")
          )
        )
      )
    )(apply)(unapply)
  )

  // TODO probably a better way to handle this, come back and see
  private def apply(itemTypes: Seq[ItemType], maybeDetails: Option[String]): Products = (itemTypes, maybeDetails) match {
    case (items, Some(detail)) if items.contains(Other("")) =>
      val modifiedTransactions = items.map(service => if (service == Other("")) Other(detail) else service)
      Products(modifiedTransactions.toSet)
    case (items, Some(_)) if !items.contains(Other("")) => throw new IllegalArgumentException("Cannot have product details without Other HVD product")
    case (items, None) if items.contains(Other("")) => throw new IllegalArgumentException("Cannot have Other HVD product without product details")
    case (items, None) if !items.contains(Other("")) => Products(items.toSet)
  }

  private def unapply(obj: Products): Option[(Seq[ItemType], Option[String])] = {
    val objTypes = obj.items.toSeq.map { x =>
      if (x.isInstanceOf[Other]) Other("") else x
    }

    val maybeName = obj.items.find(_.isInstanceOf[Other]).flatMap {
      case Other(details) => Some(details)
      case _ => None
    }

    Some((objTypes, maybeName))
  }
}
