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

package views.declaration

import cats.implicits._
import forms.{EmptyForm, Form2}
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

class select_business_nominated_officerSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val people = Seq(
      ResponsiblePeople(PersonName("Test", None, "Person1").some),
      ResponsiblePeople(PersonName("Test", None, "Person2").some)
    )
  }

  "select_business_nominated_officer view" must {
    "have correct title, headings and content" in new ViewFixture {

      def view = views.html.declaration.select_business_nominated_officer("subheading", EmptyForm, Seq.empty[ResponsiblePeople])

      doc.title mustBe s"${Messages("declaration.who.is.business.nominated.officer")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      heading.html must be(Messages("declaration.who.is.business.nominated.officer"))
      subHeading.html must include("subheading")
      doc.text() must include(Messages("declaration.who.is.business.nominated.officer.text"))
    }

    "have a list of responsible people" in new ViewFixture {



      def view = views.html.declaration.select_business_nominated_officer("subheading", EmptyForm, people)

      people map(_.personName.get) foreach { n =>
        val id = s"value-${n.fullNameWithoutSpace}"
        val e = doc.getElementById(id)

        Option(e) must be(defined)
        e.`val` mustBe s"${n.firstName}${n.lastName}"

        val label = doc.select(s"label[for=$id]")
        label.text() must include(n.fullName)
      }

    }

    "prepopulate the selected nominated officer" in new ViewFixture {

      val f = Form2(BusinessNominatedOfficer("TestPerson1"))

      def view = views.html.declaration.select_business_nominated_officer("subheading", f, people)

      doc.select("input[type=radio][id=value-TestPerson1").hasAttr("checked") mustBe true

    }

    "show the 'register someone else' radio button" in new ViewFixture {
      def view = views.html.declaration.select_business_nominated_officer("subheading", EmptyForm, Seq.empty[ResponsiblePeople])

      Option(doc.select("input[type=radio][id=value--1]")) must be(defined)
      doc.select("label[for=value--1]").text() must include(Messages("lbl.register.some.one.else"))
    }

  }
}
