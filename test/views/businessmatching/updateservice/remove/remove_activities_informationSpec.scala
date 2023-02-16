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

package views.businessmatching.updateservice.remove

import org.scalatest.MustMatchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.remove.remove_activities_information

class remove_activities_informationSpec extends AmlsViewSpec with MockitoSugar with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val remove_activities_information = app.injector.instanceOf[remove_activities_information]
    implicit val requestWithToken = addTokenForView()
    def view = remove_activities_information("placeholder")
  }

  "remove_activities view" must {

    "display the correct headings and title" in new ViewFixture {

      doc.title must include(Messages("updateservice.removeactivitiesinformation.title") + " - " + Messages("summary.updateinformation"))
      heading.html must include(Messages("updateservice.removeactivitiesinformation.header", "placeholder"))
      subHeading.html must include(Messages("summary.updateinformation"))
      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }


    "Check button redirects to status page" in new ViewFixture {
      doc.html must include(controllers.routes.StatusController.get(false).url)
    }

  }

  it when {
    "placeholder is all services" must {
      "contain all services content" in new ViewFixture {

        override def view = remove_activities_information("all services")

        doc.html must not include Messages("updateservice.removeactivitiesinformation.info.3")
        doc.html must include(Messages("updateservice.removeactivitiesinformation.info.1.all"))
        doc.html must include(Messages("updateservice.removeactivitiesinformation.info.2"))
      }
      "placeholder is not all services" must {
        "contain non services content" in new ViewFixture {
          doc.html must not include Messages("updateservice.removeactivitiesinformation.info.1.all")
          doc.html must include(Messages("updateservice.removeactivitiesinformation.info.3"))
          doc.html must include(Messages("updateservice.removeactivitiesinformation.info.2"))
        }
      }
    }
  }
}
