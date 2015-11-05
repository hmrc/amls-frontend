/*global $*/
// TODO: Tidy up
$(function () {
    // avoids double panel-indented sections
    // should be CSS but isn't possible.
    $('.panel-indent > .form-field--error').parent().css({
        'padding' : 0,
        'border' : 'none'
    });

    // show/hide
    (function () {
     var checkedInputs = 'input[type="checkbox"], input[type="radio"]';

      function showHide(bool) {
        return function () {
          var $self = $(this),
            $target = $($self.data('toggle-' + bool)),
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

          if ($inputs.filter(pred).length === 0) {
            if ($self.prop('checked') === false) {
              $target.hide();
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

          $('input[name="' + $self.prop('name') + '"][value!="' + bool + '"]').change(function() {
            hide();
          });
        };
      }

      $('*[data-toggle-true]').each(showHide(true));
      $('*[data-toggle-false]').each(showHide(false));
      $('*[data-toggle-other]').each(showHide('other'));
      })();
});
