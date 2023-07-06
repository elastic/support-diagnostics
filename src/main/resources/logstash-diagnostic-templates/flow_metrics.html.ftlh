<!--
 Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
  or more contributor license agreements. Licensed under the Elastic License
  2.0; you may not use this file except in compliance with the Elastic License
  2.0.
-->
<!DOCTYPE html>
<html lang="en">
<head>
    <title>Logstash Diagnostic</title>
    <meta charset="utf-8"/>
    <meta content="initial-scale=1, width=device-width" name="viewport"/>
    <script crossorigin="anonymous"
            src="https://unpkg.com/react@latest/umd/react.development.js"></script>
    <script src="https://unpkg.com/react-dom@latest/umd/react-dom.development.js"></script>
    <script crossorigin="anonymous"
            src="https://unpkg.com/@mui/material@latest/umd/material-ui.development.js"></script>
    <script crossorigin="anonymous"
            src="https://unpkg.com/babel-standalone@latest/babel.min.js"></script>
    <link href="https://fonts.googleapis.com/css?family=Roboto:300,400,500,700&display=swap"
          rel="stylesheet"/>
    <link href="https://fonts.googleapis.com/icon?family=Material+Icons"
          rel="stylesheet"/>
</head>
<body>
<div id="root"></div>
<script type="text/babel">

    const {
        InputLabel,
        MenuItem,
        FormControl,
        Select,
        colors,
        CssBaseline,
        ThemeProvider,
        Typography,
        createTheme,
        Box,
        Link,
        Grid,
        List,
        Divider,
        Icon,
        TextField,
        Stack,
        Card,
        CardHeader,
        CardContent,
        AppBar,
        Toolbar,
        Paper,
        Table,
        TableContainer,
        TableHead,
        TableRow,
        TableCell,
        TableBody,
        TableSortLabel,
        InputAdornment,
        IconButton,
        Tooltip,
    } = MaterialUI;

    const theme = createTheme({});

    // Generated
    const data = ${JSON.writeValueAsString(data)?no_esc};

    const SearchInput = ({value, onChange, placeholder}) => {
        const [textValue, setTextValue] = React.useState(value || '');

        const handleTextChange = (event) => {
            setTextValue(event.target.value);
            onChange(event);
        };

        return (
            <TextField
                hiddenLabel
                variant="outlined"
                size="small"
                fullWidth
                value={textValue}
                placeholder={placeholder || 'Search by prefix'}
                onChange={handleTextChange}
                InputProps={{
                    startAdornment: (
                        <InputAdornment position="start">
                            <Icon aria-hidden focusable={false} viewBox="0 0 24 24"
                                  width="16" height="16">search</Icon>
                        </InputAdornment>
                    )
                }}
            />
        );
    };

    const createDescendingComparator = (a, b, orderBy) => {
        if (b[orderBy] < a[orderBy]) {
            return -1;
        }
        if (b[orderBy] > a[orderBy]) {
            return 1;
        }
        return 0;
    }

    const createSortComparator = (orderDirection, orderBy) => {
        return orderDirection === 'desc'
            ? (a, b) => createDescendingComparator(a, b, orderBy)
            : (a, b) => -createDescendingComparator(a, b, orderBy);
    }

    const _try = (func, fallbackValue = "") => {
        try {
            let value = func();
            return (value === null || value === undefined) ? fallbackValue : value;
        } catch (e) {
            return fallbackValue;
        }
    }

    const createComparisonFormatter = (analysisWindow, flow, flowMetricName, Component = LifetimePercentageComparisonValue) => {
        let lifetime = _try(() => flow[flowMetricName].lifetime, undefined);
        let current = _try(() => flow[flowMetricName][analysisWindow], undefined);

        if (typeof lifetime === 'undefined'||
            (typeof current === 'undefined' || current === '')) {
            return <Tooltip title="Metric not availible for the selected analysis window">
                <Icon>remove</Icon>
            </Tooltip>;
        }

        if (lifetime === "Infinity") {
            lifetime = Infinity;
        }

        if (current === "Infinity") {
            current = Infinity;
        }

        return <Component lifetime={lifetime} current={current} />
    }

    const LifetimeComparisonValue = ({lifetime, current}) => {
        let difference;
        if (current === Infinity || lifetime === Infinity) {
            difference = Infinity
        } else {
            difference = current - lifetime;
        }

        let color = difference < 0 ? 'red' : difference > 0 ? 'green' : 'gray';
        let tooltipTitle = "Compared to the lifetime value " + lifetime.toFixed(5);

        return (
            <Stack direction="row" alignItems="center" spacing={1}>
                <Typography variant="h7" color="text.secondary">
                    {current}
                </Typography>
                <Tooltip title={tooltipTitle}>
                    <Typography variant="caption" color={color}>
                        {difference === Infinity ? Infinity : (difference > 0 ? '+' + difference.toFixed(5) : difference.toFixed(5))}
                    </Typography>
                </Tooltip>
                { current !== lifetime && difference !== 0 &&
                    <Tooltip title={tooltipTitle}>
                        <Icon sx={{ color: color, marginLeft: '-0.4px!important', marginTop: '-1px!important', fontSize: 12 }}>{difference > 0 ? 'arrow_upward' : 'arrow_downward'}</Icon>
                    </Tooltip>
                }
            </Stack>
        )
    }

    const LifetimePercentageComparisonValue = ({lifetime, current}) => {
        let difference;
        if (current === Infinity || lifetime === Infinity) {
            difference = Infinity
        } else {
            difference = (100 * (current - lifetime)/((current + lifetime)/2));
        }

        let color = difference < 0 ? 'red' : difference > 0 ? 'green' : 'gray';
        let tooltipTitle = "Compared to the lifetime value " + lifetime.toFixed(4) + "%";

        return (
            <Stack direction="row" alignItems="center" spacing={1}>
                <Typography variant="h7" color="text.secondary">
                    {current}
                </Typography>
                <Tooltip title={tooltipTitle}>
                    <Typography variant="caption" color={color}>
                        {difference === Infinity ? Infinity : (difference > 0 ? '+' + difference.toFixed(4) : difference.toFixed(4)) + '%'}
                    </Typography>
                </Tooltip>
                { current !== lifetime && difference !== 0 &&
                    <Tooltip title={tooltipTitle}>
                        <Icon sx={{ color: color, marginLeft: '-0.4px!important', marginTop: '-1px!important', fontSize: 12 }}>{difference > 0 ? 'arrow_upward' : 'arrow_downward'}</Icon>
                    </Tooltip>
                }
            </Stack>
        )
    }

    const PipelinesAnalysisTable = ({analysisWindow, selectableColumn = 'name', onSelectableColumnClick}) => {
        const [orderDirection, setOrderDirection] = React.useState('asc');
        const [orderBy, setOrderBy] = React.useState('worker_utilization');
        const [searchQuery, setSearchQuery] = React.useState('');

        const createPipelineComparisonFormatter = (pipeline, flowMetricName, component = LifetimePercentageComparisonValue) => {
            return createComparisonFormatter(analysisWindow, _try(() => data.stats.pipelines[pipeline].flow, {}), flowMetricName, component);
        }

        const createCombinedComparisonFormatter = (flowMetricName, component = LifetimePercentageComparisonValue) => {
            return createComparisonFormatter(analysisWindow, _try(() => data.stats.flow, {}), flowMetricName, component);
        }

        const columns = [
            {id: 'name', label: 'Pipeline', minWidth: 170},
            {id: 'input', label: 'Input', minWidth: 100, format: (row) => createPipelineComparisonFormatter(row.name, 'input_throughput')},
            {id: 'filter', label: 'Filter', minWidth: 100, format: (row) => createPipelineComparisonFormatter(row.name, 'filter_throughput')},
            {id: 'output', label: 'Output', minWidth: 100, format: (row) => createPipelineComparisonFormatter(row.name, 'output_throughput')},
            {id: 'queue_backpressure', label: 'Queue Backpressure', minWidth: 100, format: (row) => createPipelineComparisonFormatter(row.name, 'queue_backpressure')},
            {id: 'worker_concurrency', label: 'Worker concurrency', minWidth: 100, format: (row) => createPipelineComparisonFormatter(row.name, 'worker_concurrency', LifetimeComparisonValue)},
        ];

        let rows = [];
        for (let name in data.stats.pipelines) {
            rows.push({...data.stats.pipelines[name], id: name, name: name});
        }

        let selectableColumnIndex = columns.findIndex((c) => c.id === selectableColumn);
        if (selectableColumnIndex > -1) {
            columns[selectableColumnIndex] = {
                ...columns[selectableColumnIndex],
                format: (row, value) =>
                    <Link component="button" onClick={() => {
                        onSelectableColumnClick && onSelectableColumnClick(row)
                    }}>
                        {value}
                    </Link>
            }
        }

        const createSortHandler = (property) => (event) => {
            const isAsc = orderBy === property && orderDirection === 'asc';
            setOrderDirection(isAsc ? 'desc' : 'asc');
            setOrderBy(property);
        };

        const visibleRows = React.useMemo(() =>
                rows.filter((row) => searchQuery === '' || row[selectableColumn].toLowerCase().indexOf(searchQuery.toLowerCase()) > -1)
                    .sort((a, b) => {
                        const order = createSortComparator(orderDirection, orderBy)(a, b);
                        if (order !== 0) {
                            return order;
                        }
                        return a[1] - b[1];
                    }),
            [orderDirection, orderBy, searchQuery, rows],
        );

        return (
            <Paper sx={{width: '99%'}}>
                <TableContainer sx={{maxHeight: 600}}>
                    <Table stickyHeader aria-label="sticky table">
                        <TableHead>
                            <TableRow>
                                <TableCell colSpan={5}>
                                    <Typography
                                        sx={{flex: '1 1 100%'}}
                                        variant="h6"
                                        component="div"
                                        color="text.primary"
                                    >
                                        Pipelines
                                    </Typography>
                                </TableCell>
                                <TableCell colSpan={1}>
                                    <SearchInput value=""
                                                 onChange={(event) => setSearchQuery(event.target.value)}
                                                 placeholder="Search pipeline"/>
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                <TableCell align="center" colSpan={4}>
                                    Throughput
                                </TableCell>
                                <TableCell align="left" colSpan={2}>
                                    <Divider light orientation="vertical" flexItem/>
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                {columns.map((column) => (
                                    <TableCell
                                        key={column.id}
                                        align={column.align}
                                        style={{top: 57, minWidth: column.minWidth}}
                                        sortDirection={orderBy === column.id ? orderDirection : false}
                                    >
                                        <TableSortLabel
                                            active={orderBy === column.id}
                                            direction={orderBy === column.id ? orderDirection : 'asc'}
                                            onClick={createSortHandler(column.id)}
                                        >
                                            {column.label}
                                        </TableSortLabel>
                                    </TableCell>
                                ))}
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {visibleRows.map((row) => {
                                return (
                                    <TableRow hover role="checkbox"
                                              tabIndex={-1}
                                              key={row.id}>
                                        {columns.map((column) => {
                                            const value = row[column.id];
                                            return (
                                                <TableCell key={column.id}
                                                           align={column.align}>
                                                    {column.format ? column.format(row, value) : value}
                                                </TableCell>
                                            );
                                        })}
                                    </TableRow>
                                );
                            })}

                            <TableRow sx={{
                                '&:last-child td, &:last-child th': {
                                    borderTop: 1,
                                    borderBottom: 0,
                                    borderColor: 'gray'
                                },
                            }}>
                                <TableCell>
                                    Combined
                                </TableCell>
                                <TableCell>
                                    {createCombinedComparisonFormatter('input_throughput')}
                                </TableCell>
                                <TableCell>
                                    {createCombinedComparisonFormatter('filter_throughput')}
                                </TableCell>
                                <TableCell>
                                    {createCombinedComparisonFormatter('output_throughput')}
                                </TableCell>
                                <TableCell>
                                    {createCombinedComparisonFormatter('queue_backpressure')}
                                </TableCell>
                                <TableCell>
                                    {createCombinedComparisonFormatter('worker_concurrency', LifetimeComparisonValue)}
                                </TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>);
    }

    const SelectedPipelinePluginAnalysisTable = ({
                                                     title = "Plugins",
                                                     analysisWindow,
                                                     selectedPipeline,
                                                     filter,
                                                     hideFilters = false,
                                                     headerTableRowTitle,
                                                     columnsDefinition = [],
                                                 }) => {

        const [orderDirection, setOrderDirection] = React.useState('asc');
        const [orderBy, setOrderBy] = React.useState('worker_utilization');
        const [searchQuery, setSearchQuery] = React.useState('');
        const [pluginTypeFilter, setPluginTypeFilter] = React.useState('all');

        const columns = [
            {
                id: 'name', label: 'Name', minWidth: 100, format: (row, value) => {
                    return <Stack direction="row" spacing={1} alignItems="center">
                        <Tooltip title={'Plugin ID: ' + row.id}>
                            <Icon sx={{color: colors.grey[400], fontSize: 14}}>info</Icon>
                        </Tooltip>
                        <div>{value}</div>
                    </Stack>
                }
            },
            { id: 'type', label: 'Type', minWidth: 100 },
            ...columnsDefinition,
        ];


        let inputs = _try(() => data.stats.pipelines[selectedPipeline.name].plugins.inputs, [])
            .map((p) => Object.assign(p, { type: 'input', statsProperty: 'inputs' }) );

        let filters = _try(() => data.stats.pipelines[selectedPipeline.name].plugins.filters, [])
            .map((p) => Object.assign(p, { type: 'filter', statsProperty: 'filters' }) );

        let outputs = _try(() => data.stats.pipelines[selectedPipeline.name].plugins.outputs, [])
            .map((p) => Object.assign(p, { type: 'output', statsProperty: 'outputs' }) );

        let rows = [...inputs, ...filters, ...outputs];

        const createSortHandler = (property) => (event) => {
            const isAsc = orderBy === property && orderDirection === 'asc';
            setOrderDirection(isAsc ? 'desc' : 'asc');
            setOrderBy(property);
        };

        const visibleRows = React.useMemo(() =>
                rows
                    .filter((row) => (searchQuery === '' || row.name.toLowerCase().indexOf(searchQuery.toLowerCase()) > -1) &&
                        (pluginTypeFilter === 'all' || row.type === pluginTypeFilter) &&
                        (filter === undefined || filter(row) === true))
                    .sort((a, b) => {
                        const order = createSortComparator(orderDirection, orderBy)(a, b);
                        if (order !== 0) {
                            return order;
                        }
                        return a[1] - b[1];
                    }),
            [orderDirection, orderBy, searchQuery, pluginTypeFilter, rows, analysisWindow],
        );

        return (
            <Paper sx={{width: '99%'}}>
                <TableContainer sx={{maxHeight: 600}}>
                    <Table stickyHeader aria-label="sticky table">
                        <TableHead>
                            <TableRow>
                                <TableCell colSpan={columns.length - 1}>
                                    <Typography
                                        sx={{flex: '1 1 100%'}}
                                        variant="h7"
                                        component="div"
                                        color="text.secondary"
                                    >
                                        {title}
                                    </Typography>
                                </TableCell>
                                <TableCell colSpan={1}>
                                    {hideFilters === false &&
                                        <Stack direction="row" spacing={1}>
                                            <SearchInput
                                                onChange={(event) => setSearchQuery(event.target.value)}
                                                placeholder="Search plugin"/>
                                            <FormControl sx={{ minWidth: 125 }} size="small">
                                                <InputLabel id="pipeline-plugin-type-filter">Plugin type</InputLabel>
                                                <Select
                                                    labelId="pipeline-plugin-type-filter"
                                                    id="demo-select-small"
                                                    value={pluginTypeFilter}
                                                    label="Plugin type"
                                                    onChange={(event)=> setPluginTypeFilter(event.target.value)}
                                                >
                                                    <MenuItem value="all">
                                                        <em>All</em>
                                                    </MenuItem>
                                                    <MenuItem value="input">Input</MenuItem>
                                                    <MenuItem value="filter">Filter</MenuItem>
                                                    <MenuItem value="output">Output</MenuItem>
                                                </Select>
                                            </FormControl>
                                        </Stack>
                                    }
                                </TableCell>
                            </TableRow>
                            {headerTableRowTitle &&
                                <TableRow>
                                    <TableCell align="center" colSpan={4}>
                                        {headerTableRowTitle}
                                    </TableCell>
                                    <TableCell align="left" colSpan={2}>
                                        <Divider light orientation="vertical" flexItem/>
                                    </TableCell>
                                </TableRow>
                            }
                            <TableRow>
                                {columns.map((column) => (
                                    <TableCell
                                        key={column.id}
                                        align={column.align}
                                        style={{top: 57, minWidth: column.minWidth, maxWidth: column.maxWidth}}
                                        sortDirection={orderBy === column.id ? orderDirection : false}
                                    >
                                        <TableSortLabel
                                            active={orderBy === column.id}
                                            direction={orderBy === column.id ? orderDirection : 'asc'}
                                            onClick={createSortHandler(column.id)}
                                        >
                                            {column.label}
                                        </TableSortLabel>
                                    </TableCell>
                                ))}
                            </TableRow>
                        </TableHead>
                        <TableBody>
                            {visibleRows.map((row) => {
                                return (
                                    <TableRow hover role="checkbox"
                                              tabIndex={-1}
                                              key={row.id}>
                                        {columns.map((column) => {
                                            const value = row[column.id];
                                            return (
                                                <TableCell key={column.id}
                                                           align={column.align}>
                                                    {column.format ? column.format(row, value) : value}
                                                </TableCell>
                                            );
                                        })}
                                    </TableRow>
                                );
                            })}
                        </TableBody>
                    </Table>
                </TableContainer>
            </Paper>);
    }

    const GridItem = ({label, data}) => <Grid item xs={12}>
        <Grid container>
            <Grid item xs={5}>
                <Typography variant="caption" fontWeight="bolder"
                >
                    {label}
                </Typography>
            </Grid>
            <Grid item xs={7}>
                <Typography variant="caption" fontWeight="bolder"
                            color="text.secondary"
                >
                    {data}
                </Typography>
            </Grid>
        </Grid>
    </Grid>;

    const GridSubHeader = ({label}) => <Grid item xs={12} sx={{marginTop: 2}}>
        <Grid container>
            <Typography variant="caption"
                        fontWeight="bolder"
                        color="text.secondary"
            >
                {label}
            </Typography>
        </Grid>
        <Divider/>
    </Grid>;

    const ReportInformationBox = () => {
        return (
            <Grid container spacing={1}>
                <Grid item xs={12}>
                    <Card variant="outlined" sx={{
                        height: '100%',
                        border: 'inherited',
                        borderColor: theme.palette.primary[200] + 75,
                        ':hover': {
                            boxShadow: '0 2px 14px 0 rgb(32 40 45 / 8%)'
                        }
                    }}>
                        <CardHeader
                            title={<Typography sx={{fontSize: 18}} color="text.primary">
                                Report
                            </Typography>}/>
                        <Divider/>
                        <CardContent>
                            <Box sx={{flexGrow: 1}}>
                                <Grid container>
                                    <GridItem label="Diagnostic Version" data={_try(() => data.version)}/>
                                    <GridItem label="Collection Date" data={_try(() => data.createdDate)} />
                                    <GridSubHeader label="Logstash"/>
                                    <GridItem label="Host" data={_try(() => data.info.host)} />
                                    <GridItem label="Version" data={_try(() => data.info.version)} />
                                    <GridItem label="Uptime (ms)" data={_try(() => data.stats.jvm.uptime_in_millis)} />
                                    <GridSubHeader label="Pipeline" />
                                    <GridItem label="Workers" data={_try(() => data.info.pipeline.workers)} />
                                    <GridItem label="Batch size" data={_try(() => data.info.pipeline.batch_size)} />
                                    <GridItem label="Batch delay" data={_try(() => data.info.pipeline.batch_delay)} />
                                    <GridSubHeader label="OS" />
                                    <GridItem label="Name" data={_try(() => data.info.os.name)} />
                                    <GridItem label="Available Processors" data={_try(() => data.info.os.available_processors)} />
                                    <GridItem label="JVM version" data={_try(() => data.info.jvm.version)} />
                                    <GridItem label="CPU usage (%)" data={_try(() => data.stats.process.cpu.percent)} />
                                    <GridSubHeader label="Events (all)" />
                                    <GridItem label="In" data={_try(() => data.stats.events.in)} />
                                    <GridItem label="filtered" data={_try(() => data.stats.events.filtered)} />
                                    <GridItem label="Out" data={_try(() => data.stats.events.out)} />
                                </Grid>
                            </Box>
                        </CardContent>
                    </Card>
                </Grid>
            </Grid>
        );
    }

    const AnalysisWindowSelect = ({value, onValueChange}) => {
        const handleChange = (event) => {
            onValueChange(event.target.value);
        };

        return (
            <Box sx={{minWidth: 120}}>
                <FormControl fullWidth size="small" sx={{m: 1, width: 400, mt: 3}}>
                    <InputLabel id="demo-simple-select-label">Analysis window</InputLabel>
                    <Select
                        labelId="demo-simple-select-label"
                        id="demo-simple-select"
                        value={value}
                        label="Analysis window"
                        onChange={handleChange}
                    >
                        <MenuItem value="current">Most recent ~10s</MenuItem>
                        <MenuItem value="last_1_minute">Most recent ~1 minute</MenuItem>
                        <MenuItem value="last_5_minutes">Most recent ~5 minute</MenuItem>
                        <MenuItem value="last_15_minutes">Most recent ~15
                            minute</MenuItem>
                        <MenuItem value="last_1_hour">Most recent ~1 hour</MenuItem>
                        <MenuItem value="last_24_hours">Most recent ~24 hours</MenuItem>
                        <MenuItem value="lifetime">Lifetime of the relevant pipeline or
                            process</MenuItem>
                    </Select>
                </FormControl>
            </Box>
        );
    }

    const PipelineDetailsPage = ({analysisWindow, selectedPipeline, setSelectedPipeline}) => {
        let inputPluginsColumns = [
            { id: 'throughput', label: 'Throughput', minWidth: 100, format: (row) =>
                    createComparisonFormatter(
                        analysisWindow,
                        _try(() => data.stats.pipelines[selectedPipeline.name].plugins.inputs.find((p) => p.id === row.id).flow),
                        'throughput',
                        LifetimeComparisonValue
                    )
            }
        ]

        const createPipelinePluginComparisonFormatter = (row, flowMetricName, component = LifetimePercentageComparisonValue) => {
            return createComparisonFormatter(analysisWindow, row.flow, flowMetricName, component);
        }

        let otherPluginsColumns = [
            {id: 'worker_millis_per_event', label: 'Worker millis per event', minWidth: 100, format: (row) => createPipelinePluginComparisonFormatter(row, 'worker_millis_per_event', LifetimeComparisonValue)},
            {id: 'worker_utilization', label: 'Worker utilization', minWidth: 100, format: (row) => createPipelinePluginComparisonFormatter(row, 'worker_utilization', LifetimeComparisonValue)},
        ]

        return (
            <Stack spacing={2}>
                <Stack direction="row" spacing={1} alignItems="center" justifyContent="flex-start" sx={{paddingBottom: 1, paddingTop: 1}}>
                    <IconButton
                        size="small"
                        onClick={() => setSelectedPipeline && setSelectedPipeline('')}>
                        <Icon aria-hidden focusable={false}
                              viewBox="0 0 24 24"
                              width="16" height="16">arrow_back</Icon>
                    </IconButton>
                    <Typography
                        sx={{flex: '1 1 100%'}}
                        variant="h6"
                        component="div"
                        color="text.primary"
                    >
                        {selectedPipeline.name}
                    </Typography>
                </Stack>
                <Paper sx={{width: '99%'}} >
                    <Card variant="outlined" sx={{
                        height: '100%',
                        border: 'inherited',
                        borderColor: theme.palette.primary[200] + 75,
                        ':hover': {
                            boxShadow: '0 2px 14px 0 rgb(32 40 45 / 8%)'
                        }
                    }}>
                        <CardHeader title={<Typography
                            sx={{flex: '1 1 100%'}}
                            variant="h7"
                            component="div"
                            color="text.secondary"
                        >
                            Pipeline
                        </Typography>} />
                        <Divider />
                        <CardContent >
                            <Stack direction="row" spacing={2}>
                                <Typography variant="caption" fontWeight="bolder">Workers:</Typography>
                                <Typography variant="caption" fontWeight="bolder" color="text.secondary">
                                    {_try(() => data.info.pipelines[selectedPipeline.name].workers )}
                                </Typography>
                                <Divider orientation="vertical" flexItem/>
                                <Typography variant="caption" fontWeight="bolder">Batch size:</Typography>
                                <Typography variant="caption" fontWeight="bolder" color="text.secondary">
                                    {_try(() => data.info.pipelines[selectedPipeline.name].batch_size )}
                                </Typography>
                                <Divider orientation="vertical" flexItem/>
                                <Typography variant="caption" fontWeight="bolder">Events In:</Typography>
                                <Typography variant="caption" fontWeight="bolder" color="text.secondary">
                                    {_try(() => data.stats.pipelines[selectedPipeline.name].events.in )}
                                </Typography>
                                <Divider orientation="vertical" flexItem/>
                                <Typography variant="caption" fontWeight="bolder">Events Filtered:</Typography>
                                <Typography variant="caption" fontWeight="bolder" color="text.secondary">
                                    {_try(() => data.stats.pipelines[selectedPipeline.name].events.filtered )}
                                </Typography>
                                <Divider orientation="vertical" flexItem/>
                                <Typography variant="caption" fontWeight="bolder">Events Out:</Typography>
                                <Typography variant="caption" fontWeight="bolder" color="text.secondary">
                                    {_try(() => data.stats.pipelines[selectedPipeline.name].events.out )}
                                </Typography>
                                <Divider orientation="vertical" flexItem/>
                                <Typography variant="caption" fontWeight="bolder">Queue push duration (ms):</Typography>
                                <Typography variant="caption" fontWeight="bolder" color="text.secondary">
                                    {_try(() => data.stats.pipelines[selectedPipeline.name].events.queue_push_duration_in_millis )}
                                </Typography>
                                <Divider orientation="vertical" flexItem/>
                                <Typography variant="caption" fontWeight="bolder">Duration (ms):</Typography>
                                <Typography variant="caption" fontWeight="bolder" color="text.secondary">
                                    {_try(() => data.stats.pipelines[selectedPipeline.name].events.duration_in_millis )}
                                </Typography>
                            </Stack>
                        </CardContent>
                    </Card>
                </Paper>
                <SelectedPipelinePluginAnalysisTable selectedPipeline={selectedPipeline}
                                                     analysisWindow={analysisWindow}
                                                     title="Inputs"
                                                     filter={(row) => row.type && row.type === 'input'}
                                                     hideFilters={true}
                                                     columnsDefinition={inputPluginsColumns} />
                <SelectedPipelinePluginAnalysisTable
                    selectedPipeline={selectedPipeline}
                    analysisWindow={analysisWindow}
                    columnsDefinition={otherPluginsColumns}
                    filter={(row) => row.type && row.type !== 'input'} />
            </Stack>
        )
    }

    function App() {

        const [analysisWindow, setAnalysisWindow] = React.useState('current');
        const [selectedPipeline, setSelectedPipeline] = React.useState('');

        return (
            <div>
                <AppBar position="static" color="primary">
                    <Toolbar>
                        <svg
                            width="40"
                            height="40"
                            viewBox="0 0 40 45"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg">
                            <g>
                                <path
                                    d="M40.1527 20.9588C40.1527 17.5986 38.0653 14.6457 34.9257 13.5087C35.0615 12.7959 35.1294 12.0831 35.1294 11.3534C35.1294 5.09121 30.0382 -1.52588e-05 23.793 -1.52588e-05C20.1273 -1.52588e-05 16.7331 1.74797 14.5948 4.70088C13.5426 3.88628 12.2529 3.44505 10.9122 3.44505C7.58591 3.44505 4.88757 6.14339 4.88757 9.46965C4.88757 10.1994 5.02333 10.9122 5.26092 11.574C2.13831 12.6941 0 15.6979 0 19.0411C0 22.4183 2.10437 25.3712 5.24395 26.5083C5.10819 27.2041 5.0403 27.9338 5.0403 28.6635C5.0403 34.9088 10.1146 39.983 16.3598 39.983C20.0255 39.983 23.4196 38.2181 25.5409 35.2651C26.5931 36.0967 27.8829 36.5549 29.2236 36.5549C32.5498 36.5549 35.2482 33.8566 35.2482 30.5303C35.2482 29.8006 35.1124 29.0878 34.8748 28.4259C37.9974 27.3059 40.1527 24.3021 40.1527 20.9588Z"
                                    fill="white"></path>
                                <path
                                    d="M15.7824 17.2253L24.5732 21.2304L33.432 13.4578C33.5677 12.8129 33.6186 12.185 33.6186 11.5061C33.6186 6.04156 29.1723 1.59523 23.7077 1.59523C20.4324 1.59523 17.3946 3.20745 15.5448 5.9058L14.0684 13.5596L15.7824 17.2253Z"
                                    fill="#FED10A"></path>
                                <path
                                    d="M6.68665 26.5253C6.55088 27.1702 6.49997 27.832 6.49997 28.5108C6.49997 33.9924 10.9633 38.4387 16.4448 38.4387C19.7371 38.4387 22.8088 36.8095 24.6587 34.0942L26.1181 26.4744L24.1665 22.7408L15.3417 18.7187L6.68665 26.5253Z"
                                    fill="#24BBB1"></path>
                                <path
                                    d="M6.63518 11.3195L12.6598 12.745L13.9835 5.90583C13.1689 5.27791 12.1507 4.93849 11.0985 4.93849C8.48499 4.93849 6.34668 7.05983 6.34668 9.69029C6.34668 10.2503 6.4485 10.8104 6.63518 11.3195Z"
                                    fill="#EF5098"></path>
                                <path
                                    d="M6.10983 12.762C3.42846 13.6444 1.54471 16.224 1.54471 19.0581C1.54471 21.8243 3.25875 24.2851 5.82133 25.2694L14.2728 17.6326L12.7284 14.3233L6.10983 12.762Z"
                                    fill="#1BA9F5"></path>
                                <path
                                    d="M26.186 34.0942C27.0176 34.7221 28.0189 35.0785 29.0541 35.0785C31.6676 35.0785 33.8059 32.9571 33.8059 30.3267C33.8059 29.7497 33.7041 29.1896 33.5174 28.6805L27.5098 27.2719L26.186 34.0942Z"
                                    fill="#93C83E"></path>
                                <path
                                    d="M27.4075 25.6937L34.0261 27.238C36.7245 26.3555 38.5912 23.776 38.5912 20.9249C38.5912 18.1757 36.8772 15.6979 34.3146 14.7306L25.6595 22.3165L27.4075 25.6937Z"
                                    fill="#0077CC"></path>
                            </g>
                        </svg>
                        <Typography variant="h6" noWrap component="div"
                                    sx={{flexGrow: 1, marginLeft: 1}}>
                            Logstash - Flow Metrics Diagnostic
                        </Typography>
                    </Toolbar>
                </AppBar>
                <Grid container spacing={2}>
                    <Grid item xs={12}>
                        <Grid container spacing={2}>
                            <Grid item xs={3}>
                                <ReportInformationBox/>
                            </Grid>
                            <Grid item xs={9}>
                                <Stack>
                                    <AnalysisWindowSelect value={analysisWindow}
                                                          onValueChange={setAnalysisWindow}/>

                                    {selectedPipeline === ''
                                        ? <PipelinesAnalysisTable
                                            onSelectableColumnClick={setSelectedPipeline}
                                            analysisWindow={analysisWindow}/>
                                        : <PipelineDetailsPage
                                            selectedPipeline={selectedPipeline}
                                            setSelectedPipeline={setSelectedPipeline}
                                            analysisWindow={analysisWindow}/>
                                    }
                                </Stack>
                            </Grid>
                        </Grid>
                    </Grid>
                </Grid>
            </div>
        );
    }

    const root = ReactDOM.createRoot(document.getElementById('root'));
    root.render(
        <ThemeProvider theme={theme}>
            <CssBaseline/>
            <App/>
        </ThemeProvider>,
    );
</script>
</body>
</html>