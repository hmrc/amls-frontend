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

package views.businessmatching.updateservice.add

import forms.EmptyForm
import models.flowmanagement.AddBusinessTypeFlowModel
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture
import views.html.businessmatching.updateservice.add._


class update_services_summarySpec  extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    def view = update_services_summary(EmptyForm, AddBusinessTypeFlowModel())
  }

  "The update_services_summary view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.updateservice"))
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(Messages("title.cya"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(Messages("summary.updateservice"))
    }

    "for which business type you wish to register" must {
      "have a question title" in new ViewFixture {

      }

      "display the answer" in new ViewFixture {

      }

      "show edit link" in new ViewFixture {

      }
    }

    "for have any of your responsible people have passed the HMRC fit and proper test" must {
      "have a question title" in new ViewFixture {

      }

      "display the answer" in new ViewFixture {

      }

      "show edit link" in new ViewFixture {

      }
    }

    "if any have passed test, which responsible people have passed the HMRC fit and proper test" must {
      "have a question title" in new ViewFixture {

      }

      "display the answer" in new ViewFixture {

      }

      "show edit link" in new ViewFixture {

      }
    }

    "for will you do this business type at trading premises" must {
      "have a question title" in new ViewFixture {

      }

      "display the answer" in new ViewFixture {

      }

      "show edit link" in new ViewFixture {

      }
    }

    "if yes for will you do, which trading premises you will do this business type at" must {
      "have a question title" in new ViewFixture {

      }

      "display the answer" in new ViewFixture {

      }

      "show edit link" in new ViewFixture {

      }
    }
    
    "if adding MSB" must {
      "which services does your business provide" must {
        "have a question title" in new ViewFixture {
  
        }
  
        "display the answer" in new ViewFixture {
  
        }
  
        "show edit link" in new ViewFixture {
  
        }
      }
  
      "for what will your business do at these premises" must {
        "have a question title" in new ViewFixture {
  
        }
  
        "display the answer" in new ViewFixture {
  
        }
  
        "for the edit link" must {
          "not display when business does only one type of MSB subservice" in new ViewFixture {
  
          }
  
          "display when business does more than one type of MSB subservice" in new ViewFixture {
  
          }
        }
      }
      
      "if adding TransmittingMoney as an MSB subsector" must {
        "for does your business have a PSR number" must {
          "have a question title" in new ViewFixture {

          }

          "display the answer" in new ViewFixture {

          }

          "show edit link" in new ViewFixture {

          }
        }
      }
      
    }
  }
}