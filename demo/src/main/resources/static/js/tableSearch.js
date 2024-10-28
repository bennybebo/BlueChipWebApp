document.getElementById('tableSearch').addEventListener('input', function() {
  const filter = this.value.toLowerCase();
  const rows = document.querySelectorAll('#matchupsTable tbody tr');
  
  rows.forEach(row => {
    const rowText = row.textContent.toLowerCase();
    row.style.display = rowText.includes(filter) ? '' : 'none';
  });
});