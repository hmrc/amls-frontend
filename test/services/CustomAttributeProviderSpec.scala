/*
 * Copyright 2017 HM Revenue & Customs
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

package services

import org.jsoup.Jsoup
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec

class CustomAttributeProviderSpec extends PlaySpec with MockitoSugar{

  "CustomAttributeProvider" must {

    "correctly parse markdown with heading and bullet list" in {

      val rawText = "# Test Heading \n* bullet 1 \n* bullet 2 \n* bullet 3"

      Jsoup.parse(CustomAttributeProvider.commonMark(rawText)).html().replaceAll(" ","") must be(
        Jsoup.parse("<h1>Test Heading</h1><ul class=\"list list-bullet\"><li>bullet 1</li><li>bullet 2</li><li>bullet 3</li></ul>").html().replaceAll(" ",""))

    }

    "correctly parse ETMP derived markdown" in {

      val etmpDerived =
        """<P># Text Paragraph Heading</P><P>Following visits to the premises of Example Ltd by HMRC Officers, a review of your anti money laundering policies and procedures was carried out resulting in regulatory breaches which include, but are not restricted to, the following:</P><P>We issued you with a formal notice of your failure to comply with the money Laundering regulations, and in 2014 we subsequently provided you with comprehensive advice on how to improve the weaknesses that were identified. Following the recent visit to your premises by HMRC Officers it was evident that the business is continuing to fail in its duty to comply with the requirements of the regulations.</P><P>Your agent network has a high risk of exposure to money laundering due to poor governance despite the costly engagement of an external compliance consultancy. No evidence was found of risk assessment for any agent tested; all agents were treated the same despite differences in size, location and estimated volume.</P><P>Inspection of your AML records and procedures found serious shortfalls in internal controls, of most concern the monitoring and scrutiny of agent activity appears wholly inadequate in response to the large and unexpected volumes during the testing period (2015) and is not risk based.</P><P>There is evidence of serious compliance breaches under the MLR 2007; your response to risk demonstrated by exceptional trading across your agent network has been slow and ineffective. One of your agents was forecast to remit £2.4 million when in fact your records show that it had remitted in excess of £14million during period of registration.</P><P>Furthermore, it has come to our attention that there have been significant seizures of cash made amongst your agent network, as well as a seizure of cash destined for your Newcastle branch.</P><P>Alphanumeric Characters Heading</P><P>ABCDEFGHIJKLMNOPQRSTUVWXYZ</P><P>abcdefghijklmnopqrstuvwxyz</P><P>0123456789</P><P>Indented Bullets Heading</P><P>This decision takes into account the following parts of the Money Laundering Regulations 2007:</P><P>*<tab> Regulation 8</P><P>*<tab> Regulation 20(1)</P><P>*<tab> Regulation 20(2)</P><P>*<tab> Regulation 30(1)</P><P>*<tab> Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet Multi-Line Bullet</P><P>*<tab> Another Bullet</P><P>Common Special Characters Heading</P>"""

      Jsoup.parse(CustomAttributeProvider.commonMark(etmpDerived)).body().children().first().outerHtml() must be("<h1>Text Paragraph Heading</h1>")
      Jsoup.parse(CustomAttributeProvider.commonMark(etmpDerived)).getElementsByTag("ul").hasClass("list-bullet") must be(true)

    }
  }

}
