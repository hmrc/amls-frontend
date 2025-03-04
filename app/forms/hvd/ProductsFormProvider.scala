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

package forms.hvd

import forms.mappings.Mappings
import models.hvd.Products.Other
import models.hvd.{ItemType, Products}
import play.api.data.Form
import play.api.data.Forms.{mapping, seq}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIf

import javax.inject.Inject
import scala.jdk.CollectionConverters._

class ProductsFormProvider @Inject() () extends Mappings {

  private val checkboxError = "error.required.hvd.business.sell.atleast"

  val length                  = 255 // TODO This looks way too long
  def apply(): Form[Products] = Form[Products](
    mapping(
      "products"     -> seq(enumerable[ItemType](checkboxError, checkboxError)(Products.enumerable))
        .verifying(nonEmptySeq(checkboxError)),
      "otherDetails" -> mandatoryIf(
        _.values.asJavaCollection.contains(Other("").toString),
        text("error.required.hvd.business.sell.other.details").verifying(
          firstError(
            maxLength(length, "error.invalid.hvd.business.sell.other.details"),
            regexp(basicPunctuationRegex, "error.invalid.hvd.business.sell.other.format")
          )
        )
      )
    )(apply)(unapply)
  )

  private def apply(products: Seq[ItemType], maybeDetails: Option[String]): Products =
    (products.contains(Other("")), maybeDetails) match {
      case (true, Some(detail)) =>
        val modifiedItemTypes = products.map(itemType => if (itemType == Other("")) Other(detail) else itemType)
        Products(modifiedItemTypes.toSet)
      case (false, Some(_))     =>
        throw new IllegalArgumentException("Cannot have product details without Other HVD product")
      case (true, None)         => throw new IllegalArgumentException("Cannot have Other HVD product without product details")
      case (false, None)        => Products(products.toSet)
    }

  private def unapply(obj: Products): Option[(Seq[ItemType], Option[String])] = {
    val objTypes = obj.items.toSeq.map { item =>
      if (item.isInstanceOf[Other]) Other("") else item
    }

    val maybeName = obj.items.find(_.isInstanceOf[Other]).flatMap {
      case Other(details) => Some(details)
      case _              => None
    }

    Some((objTypes, maybeName))
  }
}
