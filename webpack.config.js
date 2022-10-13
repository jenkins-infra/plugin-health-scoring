/*
 * MIT License
 *
 * Copyright (c) 2022 Jenkins Infra
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the 'Software'), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
/* eslint no-undef: 0 */

const path = require('path');
const MiniCSSExtractPlugin = require('mini-css-extract-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const RemoveEmptyScriptsPlugin = require('webpack-remove-empty-scripts');
const CopyPlugin = require("copy-webpack-plugin");
const { CleanWebpackPlugin: CleanPlugin } = require('clean-webpack-plugin');

module.exports = (env, argv) => ({
  mode: 'development',
  entry: {
    style: [
      path.join(__dirname, 'src/main/less/index.less'),
    ],
  },
  output: {
    path: path.join(__dirname, 'target/classes/static'),
  },
  devtool:
    argv.mode === 'production'
      ? 'source-map'
      : 'inline-cheap-module-source-map',
  plugins: [
    new RemoveEmptyScriptsPlugin({}),
    new MiniCSSExtractPlugin({
      filename: '[name].css',
    }),
    new CopyPlugin({
      patterns: [
        {
          context: 'src/main/resources/images',
          from: '**/*',
          to: path.join(__dirname, 'target/classes/static/images'),
        }
      ],
    }),
    new CleanPlugin(),
  ],
  module: {
    rules: [
      {
        test: /\.(css|less)$/,
        use: [
          'style-loader',
          {
            loader: MiniCSSExtractPlugin.loader,
            options: {
              esModule: false,
            },
          },
          {
            loader: 'css-loader',
            options: {
              sourceMap: true,
              // ignore the URLS on the base styles as they are picked
              // from the src/main/webapp/images dir
              url: {
                filter: (url, resourcePath) => {
                  return !resourcePath.includes('index.less');
                },
              },
            },
          },
          {
            loader: 'postcss-loader',
            options: {
              sourceMap: true,
            },
          },
          {
            loader: 'less-loader',
            options: {
              sourceMap: true,
            },
          },
        ],
      },
      {
        test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
        type: "asset/resource",
        generator: {
          filename: "[name].[ext]",
        },
      },
    ],
  },
  optimization: {
    minimizer: [
      new CssMinimizerPlugin({
        minimizerOptions: {
          preset: [
            'default',
            {
              svgo: { exclude: true },
            },
          ],
        },
      }),
    ],
  },
  resolve: {
  },
});
