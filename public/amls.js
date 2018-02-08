/*global $*/
// TODO: Tidy up
$(function () {
  // avoids double panel-indented sections
  // should be CSS but isn't possible.
  // TODO: This needs a different approach
  $('.panel-indent > .form-field--error')
    .filter(function () {
      return $(this).siblings().not('legend').length === 0;
    })
    .parent().css({
      'padding': 0,
      'border': 'none'
    });

  $.widget('hmrc.auto', {
    _create: function () {
      var options, _select, _change, input, value;
      var _this = this

      function addOption(value) {
        $("<option data-added='true'>")
              .attr("value", value)
              .text(value)
              .prop("selected", true)
              .appendTo(_this.element)
      }

      if (this.element[0].hasAttribute("data-invalid-value")) {
        addOption(this.element.attr("data-invalid-value"))
      }

      value = this.element.find('option:selected').text();

      this.element.hide();

      this.element.parents('form').submit(function (e) {
        _change();
      });

      options = this.element
        .find('option')
        .toArray()
        .map(function (elem) {
          return {
            label: elem.text,
            value: elem.text,
            option: elem
          };
        });

      _select = function (event, ui) {
        ui.item.option.selected = true;
      };

      _change = function (event, ui) {
        options.forEach(function (e) {
          e.option.selected = e.label.toLowerCase() === input.val().toLowerCase();
        });

        if (_this.element.find('option:selected')[0].text == "" && input.val() != "") {
            addOption(input.val())
        }
      };

      input = $('<input>')
        .insertAfter(this.element)
        .val(value)
        .addClass('form-control')
        .autocomplete({
          source: options,
          select: _select,
          change: _change
        });
    }
  });

  var cookieData=GOVUK.getCookie("full-width-banner-cookie");

   if (cookieData == null) {
       $("#full-width-banner").addClass("full-width-banner--show");
   }

   $(".full-width-banner__close").on("click", function(e) {
       e.preventDefault();
       GOVUK.setCookie("full-width-banner-cookie", 1, 99999999999);
       $("#full-width-banner").removeClass("full-width-banner--show");
   });

  $('select[data-auto-complete]').auto();

  $('*[data-add-btn]').click(function () {
    $('select[data-auto-complete]').combobox();
  });

  (function () {
    $.widget('custom.addOne', {
      _create: function () {
        var $this = $(this.element);
        var text = $this.data('add-one');
        var children = $this.children();

        children
          .filter(':not(:first):not(:has(option[selected])):not(.form-field--error)')
          .addClass('js-hidden');
        var $button = $('<a href="#">' + text + '</a>').click(function (e) {
          e.preventDefault();
          $this.find('div.js-hidden:first').fadeIn(500).removeClass('js-hidden');
          if ($this.find('div.js-hidden').size() === 0) {
            $(this).hide();
          }
        });

        if ($this.find('div.js-hidden').size() > 0) {
          $this.append($button.show());
        }
      }
    });
    $('*[data-add-one]').addOne({});
  })();

  (function () {
    var checkedInputs = 'input[type="checkbox"], input[type="radio"]';

    $('input[data-toggle]').each(function () {
      var $self = $(this),
        $target = $($self.data('toggle')),
        $inputs = $target.find('input, option, selected, textarea');

      function pred() {
        var $this = $(this),
          hasValue = false;

        if ($this.is(checkedInputs)) {
          if ($this.prop('checked')) {
            hasValue = true;
          }
        } else if ($this.is('input') && $this.val() !== '') {
          hasValue = true;
        } else if ($this.is('option') && ($this.prop('selected') && $this.val() !== '')) {
          hasValue = true;
        }
        return hasValue;
      }

      if ($target.attr('data-toggle-new')) {
        if ($inputs.filter(pred).length || $self.prop('checked') === true) {
          $target.show();
        }
      } else {
        if ($inputs.filter(pred).length === 0) {
          $self.attr({
            'aria-controls': $target.attr('id'),
            'aria-expanded': 'false'
          });
          if ($self.prop('checked') === false) {
            $target.hide().attr('aria-hidden', 'true');
          }
        }
      }

      function hide() {
        $inputs.filter(checkedInputs).prop('checked', false);
        $inputs.filter('input, select, textarea').val('');
        $inputs.filter('option').prop('selected', false);
        $self.attr('aria-expanded', 'false');
        $target.hide().attr('aria-hidden', 'true');
      }

      $self.change(function () {
        if ($self.prop('checked') === true) {
          $self.attr('aria-expanded', 'true');
          $target.show().attr('aria-hidden', 'false');
        } else {
          hide();
        }
      });

      if ($self.prop('type') !== "checkbox") {
        $('input[name="' + $self.prop('name') + '"][value!="' + $self.val() + '"]').change(function () {
          hide();
        });
      }

      if ($self.prop('checked') === true) {
        $target.show();
      }
    });
  }());

  // add keystroke behaviour for anchors with role="button"
  // this is mainly for JAWS screen reader
  $('body').on('keypress','[role="button"]', function(e) {
    if ((e.which === 13) || (e.which === 32)) {
      e.preventDefault();
      this.click();
    }
  });

  // Prevent the user from typing anything other than digits
  $('#wrapper').on('keydown', '[data-digits=true]', function(e) {
      if (!e.shiftKey
          && (/[0-9]/.test(String.fromCharCode(e.keyCode)))       // digits
          || (/[0-9]/.test(String.fromCharCode(e.keyCode - 48)))  // numpad digits
          || e.keyCode == 13                                      // return
          || (e.keyCode >= 37 && e.keyCode < 41)                  // arrow keys
          || e.keyCode == 8                                       // backspace
          || e.keyCode == 9                                       // tab
          || e.keyCode == 46                                      // OSX delete (might be problematic)
          || e.keyCode == 127)                                    // delete
        return true;
      else
        e.preventDefault();
  });

  var duplicateErrorWrapper = $('.duplicate-submission-page');
  var duplicateErrorForm = duplicateErrorWrapper.find('form');

  if (duplicateErrorForm.length) {
    duplicateErrorWrapper.find('.report-error__content.js-hidden').removeClass('js-hidden');
    duplicateErrorWrapper.find('.report-error__toggle').remove();
  }

  var partialDeskproForm = $('.partial-deskpro-form');
  if(partialDeskproForm.length) {

    $('#get-help-action').remove();
    var container = $('.report-error__content');
    container.find('h2').remove();
    container.find('p').remove();

    var reportActionField = $('#report-action');
    var reportErrorField = $('#report-error');

    var css = {
        visibility: 'hidden',
        height: 0
    };

    reportActionField
        .val(partialDeskproForm.data('action-value'))
        .closest('.form-group-compound')
        .css(css);

    reportErrorField
        .val(partialDeskproForm.data('error-value'))
        .closest('.form-group-compound')
        .css(css);

    $(document.body).find('.report-error__content.js-hidden').removeClass('js-hidden');
  }

  $('[data-gov-autocomplete]').each(function() {
    openregisterLocationPicker({
      selectElement: this,
      url: '/anti-money-laundering/assets/countries'
    })

  var selectFieldName = $(this).attr('id');
  var nonSelectFieldName = selectFieldName.replace('-select','');
  $('#' + nonSelectFieldName).keydown(function(e) {
    if (e.keyCode === 13 && $(this).val() === '') {
        $('#' + selectFieldName).val('')
    }
  })

  $("button[name='submit']").click(function(){
    if($('#' + nonSelectFieldName).val() === '')
      $('#' + selectFieldName).val('');
  })

  })


  $(".autocomplete__input").addClass("form-control");

});
