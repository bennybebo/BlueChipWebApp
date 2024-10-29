document.addEventListener('DOMContentLoaded', function() {
    // Get all bookmaker checkboxes
    var bookmakerCheckboxes = document.querySelectorAll('.bookmaker-checkbox');

    bookmakerCheckboxes.forEach(function(checkbox) {
        checkbox.addEventListener('change', function() {
            var bookmakerId = checkbox.getAttribute('data-bookmaker');
            var isChecked = checkbox.checked;

            // Find all table headers and cells with data-bookmaker attribute equal to this bookmakerId
            var headers = document.querySelectorAll('th[data-bookmaker="' + bookmakerId + '"]');
            var cells = document.querySelectorAll('td[data-bookmaker="' + bookmakerId + '"]');

            // Show or hide the columns based on checkbox state
            headers.forEach(function(header) {
                header.style.display = isChecked ? '' : 'none';
            });
            cells.forEach(function(cell) {
                cell.style.display = isChecked ? '' : 'none';
            });
        });
    });
});
