document.addEventListener("DOMContentLoaded", function () {
    // Elements for odds and stakes
    const odds1Input = document.getElementById('odds1');
    const odds2Input = document.getElementById('odds2');
    const stake1Input = document.getElementById('stake1');
    const stake2Input = document.getElementById('stake2');

    // Elements for payout and result fields (now text inputs)
    const payout1Input = document.getElementById('payout1');
    const payout2Input = document.getElementById('payout2');
    const totalStakeInput = document.getElementById('totalStake');
    const totalPayoutInput = document.getElementById('totalPayout');
    const profitInput = document.getElementById('profit');

    let lastEditedStake = null;

    // Function to calculate the second stake to ensure equal payouts
    function calculateStake() {
        const odds1 = parseFloat(odds1Input.value) || 0;
        const odds2 = parseFloat(odds2Input.value) || 0;
        let stake1 = parseFloat(stake1Input.value) || 0;
        let stake2 = parseFloat(stake2Input.value) || 0;

        if (odds1 && odds2) {
            const payoutRatio1 = odds1 > 0 ? (odds1 / 100) + 1 : (100 / Math.abs(odds1)) + 1;
            const payoutRatio2 = odds2 > 0 ? (odds2 / 100) + 1 : (100 / Math.abs(odds2)) + 1;

            if (lastEditedStake === 'stake1') {
                // Recalculate stake2
                stake2 = (stake1 * payoutRatio1) / payoutRatio2;
                stake2Input.value = stake2.toFixed(2);
            } else {
                // Recalculate stake1
                stake1 = (stake2 * payoutRatio2) / payoutRatio1;
                stake1Input.value = stake1.toFixed(2);
            }
        }
        calculateResults();
    }

    // Function to calculate payouts and final results based on odds and stakes
    function calculateResults() {
        const odds1 = parseFloat(odds1Input.value) || 0;
        const odds2 = parseFloat(odds2Input.value) || 0;
        const stake1 = parseFloat(stake1Input.value) || 0;
        const stake2 = parseFloat(stake2Input.value) || 0;

        // Calculate payout ratios
        const payoutRatio1 = odds1 > 0 ? (odds1 / 100) + 1 : (100 / Math.abs(odds1)) + 1;
        const payoutRatio2 = odds2 > 0 ? (odds2 / 100) + 1 : (100 / Math.abs(odds2)) + 1;

        // Calculate payouts
        const payout1 = stake1 * payoutRatio1;
        const payout2 = stake2 * payoutRatio2;

        payout1Input.value = '$' + payout1.toFixed(2);
        payout2Input.value = '$' + payout2.toFixed(2);

        // Calculate total stake and total payout
        const totalStake = stake1 + stake2;
        const totalPayout = Math.min(payout1, payout2); // Since the goal is to have equal payouts

        totalStakeInput.value = '$' + totalStake.toFixed(2);
        totalPayoutInput.value = '$' + totalPayout.toFixed(2);

        // Calculate profit amount
        const profitAmount = totalPayout - totalStake;
        profitInput.value = '$' + profitAmount.toFixed(2);
        
        const profitPercentage = (profitAmount / totalStake) * 100;
		document.getElementById('profitPercentage').textContent = profitPercentage.toFixed(2) + '%';
    }

    // Event listeners to handle input changes
    odds1Input.addEventListener('input', calculateStake);
    odds2Input.addEventListener('input', calculateStake);

    stake1Input.addEventListener('input', function () {
        lastEditedStake = 'stake1';
        calculateStake();
    });

    stake2Input.addEventListener('input', function () {
        lastEditedStake = 'stake2';
        calculateStake();
    });
});


