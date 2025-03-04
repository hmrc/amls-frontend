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

package models.hvd

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import play.api.libs.json.Reads.StringReads
import play.api.libs.json._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem

case class Products(items: Set[ItemType]) {

  def sorted: Seq[ItemType] = {
    import Products._

    val sortedItemTypes = Seq(
      Alcohol,
      Antiques,
      Caravans,
      Cars,
      Clothing,
      Gold,
      Jewellery,
      MobilePhones,
      OtherMotorVehicles,
      ScrapMetals,
      Tobacco
    )

    def otherValue(items: Set[ItemType]): Option[ItemType] = items.collectFirst { case Other(itemType) =>
      Other(itemType)
    }

    val sortedListItems = sortedItemTypes intersect items.toSeq
    otherValue(items) map { other =>
      sortedListItems :+ other
    } getOrElse sortedListItems
  }
}

sealed trait ItemType {
  val value: String

  def getMessage(implicit messages: Messages): String = {
    import Products._

    this match {
      case Other("") => messages("hvd.products.option.12")
      case Other(x)  => x
      case itemType  => messages(s"hvd.products.option.${itemType.value}")
    }
  }
}

object Products extends Enumerable.Implicits {

  case object Alcohol extends WithName("alcohol") with ItemType {
    override val value: String = "01"
  }

  case object Tobacco extends WithName("tobacco") with ItemType {
    override val value: String = "02"
  }

  case object Antiques extends WithName("antiques") with ItemType {
    override val value: String = "03"
  }

  case object Cars extends WithName("cars") with ItemType {
    override val value: String = "04"
  }

  case object OtherMotorVehicles extends WithName("otherMotorVehicles") with ItemType {
    override val value: String = "05"
  }

  case object Caravans extends WithName("caravans") with ItemType {
    override val value: String = "06"
  }

  case object Jewellery extends WithName("jewellery") with ItemType {
    override val value: String = "07"
  }

  case object Gold extends WithName("gold") with ItemType {
    override val value: String = "08"
  }

  case object ScrapMetals extends WithName("scrapMetals") with ItemType {
    override val value: String = "09"
  }

  case object MobilePhones extends WithName("mobilePhones") with ItemType {
    override val value: String = "10"
  }

  case object Clothing extends WithName("clothing") with ItemType {
    override val value: String = "11"
  }

  case class Other(details: String) extends WithName("other") with ItemType {
    override val value: String = "12"
  }

  val all: Seq[ItemType] = Seq(
    Alcohol,
    Tobacco,
    Antiques,
    Cars,
    OtherMotorVehicles,
    Caravans,
    Jewellery,
    Gold,
    ScrapMetals,
    MobilePhones,
    Clothing,
    Other("")
  )

  def formValues(html: Html)(implicit messages: Messages): Seq[CheckboxItem] = {
    val allButOther = all
      .filterNot(_.value == Other("").value)
      .zipWithIndex
      .map { case (itemType, index) =>
        val conditional = if (itemType.value == Other("").value) Some(html) else None

        CheckboxItem(
          content = Text(itemType.getMessage),
          value = itemType.toString,
          id = Some(s"products_$index"),
          name = Some(s"products[$index]"),
          conditionalHtml = conditional
        )
      }
      .sortBy(_.content.asHtml.body)

    val otherCheckbox = CheckboxItem(
      content = Text(Other("").getMessage),
      value = Other("").toString,
      id = Some(s"products_${allButOther.length}"),
      name = Some(s"products[${allButOther.length}]"),
      conditionalHtml = Some(html)
    )

    allButOther :+ otherCheckbox
  }

  implicit val enumerable: Enumerable[ItemType] = Enumerable(all.map(v => v.toString -> v): _*)

  implicit val jsonReads: Reads[Products] =
    (__ \ "products").read[Set[String]].flatMap { x: Set[String] =>
      x.map {
        case "01" => Reads(_ => JsSuccess(Alcohol)) map identity[ItemType]
        case "02" => Reads(_ => JsSuccess(Tobacco)) map identity[ItemType]
        case "03" => Reads(_ => JsSuccess(Antiques)) map identity[ItemType]
        case "04" => Reads(_ => JsSuccess(Cars)) map identity[ItemType]
        case "05" => Reads(_ => JsSuccess(OtherMotorVehicles)) map identity[ItemType]
        case "06" => Reads(_ => JsSuccess(Caravans)) map identity[ItemType]
        case "07" => Reads(_ => JsSuccess(Jewellery)) map identity[ItemType]
        case "08" => Reads(_ => JsSuccess(Gold)) map identity[ItemType]
        case "09" => Reads(_ => JsSuccess(ScrapMetals)) map identity[ItemType]
        case "10" => Reads(_ => JsSuccess(MobilePhones)) map identity[ItemType]
        case "11" => Reads(_ => JsSuccess(Clothing)) map identity[ItemType]
        case "12" =>
          (JsPath \ "otherDetails").read[String].map(Other.apply _) map identity[ItemType]
        case _    =>
          Reads(_ => JsError((JsPath \ "products") -> play.api.libs.json.JsonValidationError("error.invalid")))
      }.foldLeft[Reads[Set[ItemType]]](
        Reads[Set[ItemType]](_ => JsSuccess(Set.empty))
      ) { (result, data) =>
        data flatMap { m =>
          result.map { n =>
            n + m
          }
        }
      }
    } map Products.apply

  implicit val jsonWrite: Writes[Products] = Writes[Products] { case Products(transactions) =>
    Json.obj(
      "products" -> (transactions map {
        _.value
      }).toSeq
    ) ++ transactions.foldLeft[JsObject](Json.obj()) {
      case (m, Other(name)) =>
        m ++ Json.obj("otherDetails" -> name)
      case (m, _)           =>
        m
    }
  }
}
