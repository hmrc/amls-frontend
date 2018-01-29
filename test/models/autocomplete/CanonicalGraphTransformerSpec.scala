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

package models.autocomplete

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import org.mockito.Mockito.when

class CanonicalGraphTransformerSpec extends PlaySpec with MockitoSugar {

  val json =
    """
      |{
      |  "country:AD": {
      |    "edges": {
      |      "from": [
      |      ]
      |    },
      |    "meta": {
      |      "canonical": true,
      |      "canonical-mask": 1,
      |      "display-name": true,
      |      "stable-name": true
      |    },
      |    "names": {
      |      "cy": false,
      |      "en-GB": "Andorra"
      |    }
      |  },
      |  "country:AE": {
      |    "edges": {
      |      "from": [
      |      ]
      |    },
      |    "meta": {
      |      "canonical": true,
      |      "canonical-mask": 1,
      |      "display-name": true,
      |      "stable-name": true
      |    },
      |    "names": {
      |      "cy": false,
      |      "en-GB": "United Arab Emirates"
      |    }
      |  },
      |  "country:AF": {
      |    "edges": {
      |      "from": [
      |      ]
      |    },
      |    "meta": {
      |      "canonical": true,
      |      "canonical-mask": 1,
      |      "display-name": true,
      |      "stable-name": true
      |    },
      |    "names": {
      |      "cy": false,
      |      "en-GB": "Afghanistan"
      |    }
      |  },
      |  "territory:AE-AZ": {
      |    "edges": {
      |      "from": [
      |      ]
      |    },
      |    "meta": {
      |      "canonical": true,
      |      "canonical-mask": 1,
      |      "display-name": true,
      |      "stable-name": true
      |    },
      |    "names": {
      |      "cy": false,
      |      "en-GB": "Abu Dhabi"
      |    }
      |  }
      |}
    """.stripMargin

  "Prune unsupported country codes from the Json" when {
    "the Json is read and transformed" in {
      val whitelist = Set("AD", "AE")
      val loader = mock[CanonicalGraphJsonLoader]
      when(loader.load) thenReturn Json.parse(json).asOpt[JsObject]

      val transformer = new CanonicalGraphTransformer(loader)
      val result = transformer.transform(whitelist)

      (result.get \ "territory:AE-AZ").toOption mustBe None
      (result.get \ "territory:AF").toOption mustBe None
    }
  }

}
