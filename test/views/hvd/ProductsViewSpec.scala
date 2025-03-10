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

package views.hvd

import forms.hvd.ProductsFormProvider
import models.hvd.Products
import models.hvd.Products.{Cars, OtherMotorVehicles}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.ProductsView

class ProductsViewSpec extends AmlsViewSpec with Matchers {

  lazy val products = inject[ProductsView]
  lazy val fp       = inject[ProductsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "products view" must {

    "have correct title" in new ViewFixture {

      def view = products(fp().fill(Products(Set(Cars))), true)

      doc.title must startWith(messages("hvd.products.title") + " - " + messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      def view = products(fp().fill(Products(Set(OtherMotorVehicles))), true)
      heading.html    must be(messages("hvd.products.title"))
      subHeading.html must include(messages("summary.hvd"))
    }

    behave like pageWithErrors(
      products(fp().withError("products", "error.required.hvd.business.sell.atleast"), false),
      "products",
      "error.required.hvd.business.sell.atleast"
    )

    behave like pageWithErrors(
      products(fp().withError("otherDetails", "error.invalid.hvd.business.sell.other.format"), false),
      "otherDetails",
      "error.invalid.hvd.business.sell.other.format"
    )

    behave like pageWithBackLink(products(fp(), false))
  }
}
