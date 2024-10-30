$(function() {
  'use strict';

  // Updated area chart data with 12 data points for YTD
  var areaData = {
    labels: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
    datasets: [
      {
        label: 'User Profit',
        data: [500, 1200, 2800, 3600, 4500, 6300, 5900, 7800, 10800, 12300, 13800, 15500], // Sample upward trend data
        backgroundColor: 'rgba(54, 162, 235, 0.2)',
        borderColor: 'rgba(54, 162, 235, 1)',
        borderWidth: 2,
        fill: true,
      },
      {
        label: 'Expected Profit',
        data: [700, 1100, 2200, 3100, 4700, 6000, 7500, 8900, 10300, 13500, 14000, 14800], // Sample expected data
        backgroundColor: 'rgba(75, 192, 192, 0.2)',
        borderColor: 'rgba(75, 192, 192, 1)',
        borderWidth: 2,
        fill: false,
      }
    ]
  };

  var areaOptions = {
    plugins: {
      filler: {
        propagate: true
      }
    },
    tooltips: {
      mode: 'index', // Shows all datasets' tooltips for a single x-axis point
      intersect: false // Disables separate hovering for each line point
    },
    scales: {
      yAxes: [{
        ticks: {
          beginAtZero: true,
          suggestedMax: 20000,
          callback: function(value) {
            return '$' + value.toLocaleString(); // Format y-axis labels as dollar amounts
          }
        },
        gridLines: {
          color: "rgba(204, 204, 204,0.1)"
        }
      }],
      xAxes: [{
        gridLines: {
          color: "rgba(204, 204, 204,0.1)"
        }
      }]
    }
  };

  // Updated doughnut chart data to represent profit by sport
  var doughnutPieData = {
    datasets: [{
      data: [4000, 3000, 2000, 4500, 2000], // These should add up to 15500
      backgroundColor: [
        'rgba(255, 99, 132, 0.5)',  // NFL
        'rgba(54, 162, 235, 0.5)',  // NCAAF
        'rgba(255, 206, 86, 0.5)',  // MLB
        'rgba(75, 192, 192, 0.5)',  // NHL
        'rgba(153, 102, 255, 0.5)'  // NBA
      ],
      borderColor: [
        'rgba(255, 99, 132, 1)',
        'rgba(54, 162, 235, 1)',
        'rgba(255, 206, 86, 1)',
        'rgba(75, 192, 192, 1)',
        'rgba(153, 102, 255, 1)'
      ],
    }],
    labels: [
      'NFL',
      'NCAAF',
      'MLB',
      'NHL',
      'NBA'
    ]
  };

  var doughnutPieOptions = {
    responsive: true,
    animation: {
      animateScale: true,
      animateRotate: true
    }
  };

  // Initialize Area Chart
  if ($("#areaChart").length) {
    var areaChartCanvas = $("#areaChart").get(0).getContext("2d");
    var areaChart = new Chart(areaChartCanvas, {
      type: 'line',
      data: areaData,
      options: areaOptions
    });
  }

  // Initialize Doughnut Chart
  if ($("#doughnutChart").length) {
    var doughnutChartCanvas = $("#doughnutChart").get(0).getContext("2d");
    var doughnutChart = new Chart(doughnutChartCanvas, {
      type: 'doughnut',
      data: doughnutPieData,
      options: doughnutPieOptions
    });
  }
});
