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

package views.declaration

import cats.implicits._
import forms.{EmptyForm, Form2}
import models.declaration.BusinessNominatedOfficer
import models.responsiblepeople.{PersonName, ResponsiblePerson}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.declaration.select_business_nominated_officer

class select_business_nominated_officerSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val select_business_nominated_officer = app.injector.instanceOf[select_business_nominated_officer]
    implicit val requestWithToken = addTokenForView()

    val people = Seq(
      ResponsiblePerson(PersonName("Test", None, "Person1").some),
      ResponsiblePerson(PersonName("Test", None, "Person2").some)
    )
  }

  "select_business_nominated_officer view" must {
    "have correct title, headings and content" in new ViewFixture {

      def view = select_business_nominated_officer("subheading", EmptyForm, Seq.empty[ResponsiblePerson])

      doc.title mustBe s"${Messages("declaration.who.is.business.nominated.officer")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      heading.html must be(Messages("declaration.who.is.business.nominated.officer"))
      subHeading.html must include("subheading")
      doc.text() must include(Messages("declaration.who.is.business.nominated.officer.text"))
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "have a list of responsible people" in new ViewFixture {



      def view = select_business_nominated_officer("subheading", EmptyForm, people)

      people.zipWithIndex.map { n =>
        n._1.personName.map { obj =>
          val id = s"value-${n._2}"
          val e = doc.getElementById(id)

          Option(e) must be(defined)
          e.`val` mustBe s"${obj.firstName}${obj.lastName}"

          val label = doc.select(s"label[for=$id]")
          label.text() must include(obj.fullName)
        }
      }
    }

    "prepopulate the selected nominated officer" in new ViewFixture {

      val f = Form2(BusinessNominatedOfficer("TestPerson1"))

      def view = select_business_nominated_officer("subheading", f, people)

      doc.select("input[type=radio][id=value-0]").hasAttr("checked") mustBe true

    }

    "show the 'register someone else' radio button" in new ViewFixture {
      def view = select_business_nominated_officer("subheading", EmptyForm, Seq.empty[ResponsiblePerson])

      Option(doc.select("input[type=radio][id=value--1]")) must be(defined)
      doc.select("label[for=value--1]").text() must include(Messages("lbl.register.some.one.else"))
    }

  }
}
