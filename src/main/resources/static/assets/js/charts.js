export function drawLineChart(canvas, points) {
    if (!canvas || !points.length) return;

    const ratio = Math.max(window.devicePixelRatio || 1, 1);
    const bounds = canvas.getBoundingClientRect();
    const width = Math.max(bounds.width, 320);
    const height = Math.max(bounds.height, 220);
    canvas.width = Math.round(width * ratio);
    canvas.height = Math.round(height * ratio);

    const context = canvas.getContext("2d");
    context.scale(ratio, ratio);
    context.clearRect(0, 0, width, height);

    const values = points.map(point => point.newRating ?? point.oldRating ?? 0);
    const minimum = Math.min(...values);
    const maximum = Math.max(...values);
    const spread = Math.max(maximum - minimum, 30);
    const low = minimum - spread * .2;
    const high = maximum + spread * .2;
    const padding = { top: 20, right: 18, bottom: 35, left: 48 };
    const chartWidth = width - padding.left - padding.right;
    const chartHeight = height - padding.top - padding.bottom;
    const x = index => padding.left + (points.length === 1
        ? chartWidth / 2
        : index * chartWidth / (points.length - 1));
    const y = value => padding.top + (high - value) / (high - low) * chartHeight;

    context.font = "10px ui-monospace, monospace";
    context.fillStyle = "#6f7a7d";
    context.strokeStyle = "#222b2f";
    context.lineWidth = 1;
    for (let index = 0; index <= 4; index += 1) {
        const value = low + (high - low) * index / 4;
        const rowY = y(value);
        context.beginPath();
        context.moveTo(padding.left, rowY);
        context.lineTo(width - padding.right, rowY);
        context.stroke();
        context.fillText(Math.round(value), 4, rowY + 3);
    }

    const gradient = context.createLinearGradient(0, padding.top, 0, height);
    gradient.addColorStop(0, "rgba(224, 165, 43, .28)");
    gradient.addColorStop(1, "rgba(224, 165, 43, 0)");
    context.beginPath();
    values.forEach((value, index) => {
        const pointX = x(index);
        const pointY = y(value);
        if (index === 0) context.moveTo(pointX, pointY);
        else context.lineTo(pointX, pointY);
    });
    context.lineTo(x(values.length - 1), height - padding.bottom);
    context.lineTo(x(0), height - padding.bottom);
    context.closePath();
    context.fillStyle = gradient;
    context.fill();

    context.beginPath();
    values.forEach((value, index) => {
        if (index === 0) context.moveTo(x(index), y(value));
        else context.lineTo(x(index), y(value));
    });
    context.strokeStyle = "#e0a52b";
    context.lineWidth = 2;
    context.stroke();

    values.forEach((value, index) => {
        context.beginPath();
        context.arc(x(index), y(value), 3, 0, Math.PI * 2);
        context.fillStyle = points[index].won ? "#58c58b" : "#e1655f";
        context.fill();
    });

    const labelIndexes = [...new Set([0, Math.floor((points.length - 1) / 2), points.length - 1])];
    context.fillStyle = "#6f7a7d";
    context.textAlign = "center";
    labelIndexes.forEach(index => {
        const date = new Date(points[index].startedAt);
        context.fillText(
            date.toLocaleDateString("it-IT", { day: "2-digit", month: "short" }),
            x(index),
            height - 10
        );
    });
}
