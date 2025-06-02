import * as echarts from 'echarts/core';
import {BarChart} from 'echarts/charts';
import {
  DatasetComponent,
  GridComponent,
  MarkLineComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components';
import {LabelLayout, UniversalTransition} from "echarts/features";
import {SVGRenderer} from 'echarts/renderers';

echarts.use([
  BarChart,
  DatasetComponent,
  GridComponent,
  LabelLayout,
  MarkLineComponent,
  SVGRenderer,
  TitleComponent,
  TooltipComponent,
  UniversalTransition,
]);

export function createChart(elementId, option = {}) {
  const chartContainer = document.getElementById(elementId);
  const chart = echarts.init(chartContainer, null, {renderer: 'svg'});
  chart.setOption(option);
  window.addEventListener('resize', () => chart.resize());
  return chart;
}
