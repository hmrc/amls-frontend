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
            'padding' : 0,
            'border' : 'none'
        });

    $.widget('custom.combobox', {

      _create: function () {

        this.wrapper = $('<span>')
          .addClass('custom-combobox')
          .insertAfter(this.element);

        this.element.hide();
        this._createAutocomplete();
      },

      _createAutocomplete: function () {

        var self = this,
          selected = this.element.children(':selected'),
          value = selected.val() ? selected.text() : '';

        $('form').submit(function () {
            console.log(self.options);
            self.input.trigger('autocompleteselect');
            return false;
        });

        this.input = $('<input>')
          .appendTo(this.wrapper)
          .val(value)
          .addClass('custom-combobox-input ui-widget ui-widget-content ui-state-default ui-corner-left')
          .autocomplete({
            delay: 0,
            minLength: 0,
            source: $.proxy(this, '_source')
          });

        this._on(this.input, {
          autocompleteselect: function (event, ui) {
            console.log(this.input.val());
            console.log(this.element.val());
            // this._trigger('select', event, {
            //   item: ui.item.option
            // });
          },
          autocompletechange: function (event, ui) {
            if (!ui.item) {
                $(this.element).val('');
            }
          }
        });
      },

      _source: function (request, response) {
        var matcher = new RegExp($.ui.autocomplete.escapeRegex(request.term), 'i');
        response( this.element.children('option').map(function () {
          var text = $( this ).text();
          if (this.value && (!request.term || matcher.test(text)))
            return {
              label: text,
              value: text,
              option: this
            };
        }));
      }
    });

    $('select.country-selector').combobox();

    $('*[data-add-btn]').click(function () {
        $('select.country-selector').combobox();
    });

    (function () {
        $.widget('custom.addOne', {
            _create: function () {
                var $this = $(this.element);
                var text = $this.data('add-one');
                var children = $this.children();

                children
                    .filter(':not(:first):not(:has(option[selected]))')
                    .addClass('js-hidden');

                var $button = $('<a href="#">' + text + '</a>').click(function (e) {
                    e.preventDefault();
                    $this.find('div.js-hidden:first').fadeIn(500).removeClass('js-hidden');
                    if ($this.find('div.js-hidden').size() === 0) {
                        $(this).hide();
                    }
                });

                $this.append($button.hide().fadeIn(1000));
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
                    if ($self.prop('checked') === false) {
                        $target.hide();
                    }
                }
            }

            function hide() {
                $inputs.filter(checkedInputs).prop('checked', false);
                $inputs.filter('input, select, textarea').val('');
                $inputs.filter('option').prop('selected', false);
                $target.hide();
            }

            $self.change(function () {
                if ($self.prop('checked') === true) {
                    $target.show();
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
});
