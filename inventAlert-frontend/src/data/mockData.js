export const companies = [
  { id: 'comp-1', companyName: 'TechWave Logistics', adminEmail: 'admin@techwave.com', status: 'ACTIVE', logo: null, createdAt: '2026-01-15T10:00:00Z' },
  { id: 'comp-2', companyName: 'GlobalStock Inc.', adminEmail: 'admin@globalstock.com', status: 'ACTIVE', logo: null, createdAt: '2026-02-20T09:00:00Z' },
  { id: 'comp-3', companyName: 'Apex Traders', adminEmail: 'admin@apex.com', status: 'SUSPENDED', logo: null, createdAt: '2026-03-01T14:00:00Z' },
  { id: 'comp-4', companyName: 'Meridian Supplies', adminEmail: 'admin@meridian.com', status: 'ACTIVE', logo: null, createdAt: '2026-04-10T11:00:00Z' },
]

export const users = [
  { id: 'user-1', companyId: 'comp-1', email: 'admin@techwave.com', name: 'Alice Johnson', role: 'ADMIN', isActive: true, createdAt: '2026-01-15T10:00:00Z' },
  { id: 'user-2', companyId: 'comp-1', email: 'manager@techwave.com', name: 'Bob Smith', role: 'MANAGER', isActive: true, createdAt: '2026-01-16T09:00:00Z' },
  { id: 'user-3', companyId: 'comp-1', email: 'staff.a@techwave.com', name: 'Charlie Brown', role: 'WAREHOUSE_STAFF', isActive: true, createdAt: '2026-01-17T08:00:00Z' },
  { id: 'user-4', companyId: 'comp-1', email: 'staff.b@techwave.com', name: 'Diana Ross', role: 'WAREHOUSE_STAFF', isActive: true, createdAt: '2026-01-17T08:30:00Z' },
  { id: 'user-5', companyId: 'comp-1', email: 'procurement@techwave.com', name: 'Eve Martinez', role: 'PROCUREMENT_OFFICER', isActive: true, createdAt: '2026-01-18T07:00:00Z' },
  { id: 'user-6', companyId: 'comp-1', email: 'inactive@techwave.com', name: 'Frank Wilson', role: 'WAREHOUSE_STAFF', isActive: false, createdAt: '2026-02-01T10:00:00Z' },
]

export const warehouseAssignments = [
  { id: 'assign-1', userId: 'user-3', companyId: 'comp-1', warehouseId: 'wh-1', assignedAt: '2026-01-23T10:00:00Z' },
  { id: 'assign-2', userId: 'user-4', companyId: 'comp-1', warehouseId: 'wh-2', assignedAt: '2026-01-23T10:30:00Z' },
  { id: 'assign-3', userId: 'user-2', companyId: 'comp-1', warehouseId: 'wh-1', assignedAt: '2026-01-20T10:00:00Z' },
  { id: 'assign-4', userId: 'user-5', companyId: 'comp-1', warehouseId: 'wh-1', assignedAt: '2026-01-20T10:00:00Z' },
]

export const warehouses = [
  { id: 'wh-1', name: 'Warehouse Alpha', address: '123 North Street, Lagos', isActive: true, createdBy: 'user-1', createdAt: '2026-01-20T10:00:00Z', updatedAt: '2026-01-20T10:00:00Z' },
  { id: 'wh-2', name: 'Warehouse Beta', address: '456 South Avenue, Abuja', isActive: true, createdBy: 'user-1', createdAt: '2026-01-20T11:00:00Z', updatedAt: '2026-01-20T11:00:00Z' },
  { id: 'wh-3', name: 'Warehouse Gamma', address: '789 East Road, Port Harcourt', isActive: false, createdBy: 'user-1', createdAt: '2026-02-01T09:00:00Z', updatedAt: '2026-02-05T10:00:00Z' },
]

export const products = [
  { id: 'prod-1', name: 'Industrial Laptop', sku: 'LAP-001', unitOfMeasure: 'units', defaultThreshold: 20, isActive: true, createdBy: 'user-1', createdAt: '2026-01-21T10:00:00Z', updatedAt: '2026-01-21T10:00:00Z' },
  { id: 'prod-2', name: 'Office Chair', sku: 'CHR-002', unitOfMeasure: 'units', defaultThreshold: 15, isActive: true, createdBy: 'user-1', createdAt: '2026-01-21T10:30:00Z', updatedAt: '2026-01-21T10:30:00Z' },
  { id: 'prod-3', name: 'Printer Ink Cartridge', sku: 'INK-003', unitOfMeasure: 'boxes', defaultThreshold: 50, isActive: true, createdBy: 'user-1', createdAt: '2026-01-21T11:00:00Z', updatedAt: '2026-01-21T11:00:00Z' },
  { id: 'prod-4', name: 'Whiteboard Marker Set', sku: 'MRK-004', unitOfMeasure: 'sets', defaultThreshold: 30, isActive: true, createdBy: 'user-1', createdAt: '2026-01-22T09:00:00Z', updatedAt: '2026-01-22T09:00:00Z' },
  { id: 'prod-5', name: 'Network Switch', sku: 'NET-005', unitOfMeasure: 'units', defaultThreshold: 10, isActive: true, createdBy: 'user-1', createdAt: '2026-01-22T10:00:00Z', updatedAt: '2026-01-22T10:00:00Z' },
]

export const stockLevels = [
  { id: 'sl-1', productId: 'prod-1', warehouseId: 'wh-1', currentStock: 12, threshold: 20, velocityPerDay: 2.5, daysUntilEmpty: 5, updatedAt: '2026-05-07T15:00:00Z' },
  { id: 'sl-2', productId: 'prod-1', warehouseId: 'wh-2', currentStock: 45, threshold: 20, velocityPerDay: 1.2, daysUntilEmpty: 37, updatedAt: '2026-05-07T15:00:00Z' },
  { id: 'sl-3', productId: 'prod-2', warehouseId: 'wh-1', currentStock: 8, threshold: 15, velocityPerDay: 1.8, daysUntilEmpty: 4, updatedAt: '2026-05-07T14:00:00Z' },
  { id: 'sl-4', productId: 'prod-2', warehouseId: 'wh-2', currentStock: 32, threshold: 15, velocityPerDay: 0.9, daysUntilEmpty: 35, updatedAt: '2026-05-07T14:00:00Z' },
  { id: 'sl-5', productId: 'prod-3', warehouseId: 'wh-1', currentStock: 65, threshold: 50, velocityPerDay: 8.3, daysUntilEmpty: 8, updatedAt: '2026-05-07T13:00:00Z' },
  { id: 'sl-6', productId: 'prod-3', warehouseId: 'wh-2', currentStock: 120, threshold: 50, velocityPerDay: 5.1, daysUntilEmpty: 23, updatedAt: '2026-05-07T13:00:00Z' },
  { id: 'sl-7', productId: 'prod-4', warehouseId: 'wh-1', currentStock: 22, threshold: 30, velocityPerDay: 4.2, daysUntilEmpty: 5, updatedAt: '2026-05-07T12:00:00Z' },
  { id: 'sl-8', productId: 'prod-4', warehouseId: 'wh-2', currentStock: 78, threshold: 30, velocityPerDay: 2.1, daysUntilEmpty: 37, updatedAt: '2026-05-07T12:00:00Z' },
  { id: 'sl-9', productId: 'prod-5', warehouseId: 'wh-1', currentStock: 6, threshold: 10, velocityPerDay: 0.8, daysUntilEmpty: 7, updatedAt: '2026-05-07T11:00:00Z' },
  { id: 'sl-10', productId: 'prod-5', warehouseId: 'wh-2', currentStock: 18, threshold: 10, velocityPerDay: 0.4, daysUntilEmpty: 45, updatedAt: '2026-05-07T11:00:00Z' },
]

export const movements = [
  { id: 'mov-1', productId: 'prod-1', warehouseId: 'wh-1', type: 'INTAKE', quantity: 50, referenceId: null, createdBy: 'user-3', createdAt: '2026-04-01T10:00:00Z' },
  { id: 'mov-2', productId: 'prod-1', warehouseId: 'wh-1', type: 'OUTBOUND_SALE', quantity: 10, referenceId: null, createdBy: 'user-3', createdAt: '2026-04-15T11:00:00Z' },
  { id: 'mov-3', productId: 'prod-1', warehouseId: 'wh-1', type: 'OUTBOUND_SALE', quantity: 8, referenceId: null, createdBy: 'user-3', createdAt: '2026-04-22T14:00:00Z' },
  { id: 'mov-4', productId: 'prod-2', warehouseId: 'wh-1', type: 'INTAKE', quantity: 30, referenceId: null, createdBy: 'user-3', createdAt: '2026-04-05T09:00:00Z' },
  { id: 'mov-5', productId: 'prod-2', warehouseId: 'wh-1', type: 'OUTBOUND_SALE', quantity: 12, referenceId: null, createdBy: 'user-3', createdAt: '2026-04-28T15:00:00Z' },
  { id: 'mov-6', productId: 'prod-3', warehouseId: 'wh-2', type: 'INTAKE', quantity: 200, referenceId: null, createdBy: 'user-4', createdAt: '2026-04-10T10:00:00Z' },
  { id: 'mov-7', productId: 'prod-3', warehouseId: 'wh-2', type: 'OUTBOUND_SALE', quantity: 35, referenceId: null, createdBy: 'user-4', createdAt: '2026-05-01T11:00:00Z' },
  { id: 'mov-8', productId: 'prod-4', warehouseId: 'wh-1', type: 'INTAKE', quantity: 60, referenceId: null, createdBy: 'user-3', createdAt: '2026-04-12T09:00:00Z' },
  { id: 'mov-9', productId: 'prod-5', warehouseId: 'wh-1', type: 'OUTBOUND_SALE', quantity: 4, referenceId: null, createdBy: 'user-3', createdAt: '2026-05-05T16:00:00Z' },
  { id: 'mov-10', productId: 'prod-1', warehouseId: 'wh-2', type: 'TRANSFER_OUT', quantity: 5, referenceId: 'trans-4', createdBy: 'user-4', createdAt: '2026-05-06T10:00:00Z' },
  { id: 'mov-11', productId: 'prod-5', warehouseId: 'wh-1', type: 'TRANSFER_IN', quantity: 8, referenceId: 'trans-4', createdBy: 'user-3', createdAt: '2026-05-06T14:00:00Z' },
  { id: 'mov-12', productId: 'prod-4', warehouseId: 'wh-1', type: 'RECONCILIATION', quantity: 3, referenceId: 'rec-2', createdBy: 'user-2', createdAt: '2026-05-06T10:00:00Z' },
]

export const transfers = [
  { id: 'trans-1', productId: 'prod-1', fromWarehouseId: 'wh-2', toWarehouseId: 'wh-1', quantity: 20, distanceKm: 524.3, distanceSource: 'GOOGLE_MAPS', status: 'SUGGESTED', approvedBy: null, createdAt: '2026-05-07T08:00:00Z', updatedAt: '2026-05-07T08:00:00Z' },
  { id: 'trans-2', productId: 'prod-2', fromWarehouseId: 'wh-2', toWarehouseId: 'wh-1', quantity: 15, distanceKm: 524.3, distanceSource: 'GOOGLE_MAPS', status: 'APPROVED', approvedBy: 'user-2', createdAt: '2026-05-06T09:00:00Z', updatedAt: '2026-05-06T12:00:00Z' },
  { id: 'trans-3', productId: 'prod-4', fromWarehouseId: 'wh-2', toWarehouseId: 'wh-1', quantity: 25, distanceKm: 524.3, distanceSource: 'HAVERSINE', status: 'IN_TRANSIT', approvedBy: 'user-2', createdAt: '2026-05-05T10:00:00Z', updatedAt: '2026-05-07T09:00:00Z' },
  { id: 'trans-4', productId: 'prod-5', fromWarehouseId: 'wh-2', toWarehouseId: 'wh-1', quantity: 8, distanceKm: 524.3, distanceSource: 'HAVERSINE', status: 'COMPLETED', approvedBy: 'user-2', createdAt: '2026-05-03T11:00:00Z', updatedAt: '2026-05-05T14:00:00Z' },
  { id: 'trans-5', productId: 'prod-3', fromWarehouseId: 'wh-1', toWarehouseId: 'wh-2', quantity: 40, distanceKm: 524.3, distanceSource: 'GOOGLE_MAPS', status: 'REJECTED', approvedBy: null, createdAt: '2026-05-04T10:00:00Z', updatedAt: '2026-05-04T15:00:00Z' },
]

export const alerts = [
  { id: 'alert-1', productId: 'prod-1', warehouseId: 'wh-1', stockAtAlert: 12, threshold: 20, status: 'OPEN', assignedTo: null, createdAt: '2026-05-07T08:10:00Z', updatedAt: '2026-05-07T08:10:00Z' },
  { id: 'alert-2', productId: 'prod-3', warehouseId: 'wh-1', stockAtAlert: 45, threshold: 50, status: 'ACKNOWLEDGED', assignedTo: 'user-5', createdAt: '2026-05-06T09:00:00Z', updatedAt: '2026-05-06T14:00:00Z' },
  { id: 'alert-3', productId: 'prod-2', warehouseId: 'wh-1', stockAtAlert: 8, threshold: 15, status: 'ORDER_PLACED', assignedTo: 'user-5', createdAt: '2026-05-05T10:00:00Z', updatedAt: '2026-05-07T10:00:00Z' },
  { id: 'alert-4', productId: 'prod-5', warehouseId: 'wh-1', stockAtAlert: 3, threshold: 10, status: 'RESOLVED', assignedTo: 'user-5', createdAt: '2026-05-01T11:00:00Z', updatedAt: '2026-05-05T16:00:00Z' },
]

export const reconciliations = [
  { id: 'rec-1', productId: 'prod-3', warehouseId: 'wh-1', systemCount: 65, physicalCount: 58, discrepancy: -7, reason: 'Found damaged units during physical count', status: 'PENDING_APPROVAL', createdBy: 'user-3', approvedBy: null, createdAt: '2026-05-07T11:00:00Z', updatedAt: '2026-05-07T11:00:00Z' },
  { id: 'rec-2', productId: 'prod-4', warehouseId: 'wh-1', systemCount: 22, physicalCount: 25, discrepancy: 3, reason: 'Previously unrecorded intake units found in back storage', status: 'APPROVED', createdBy: 'user-3', approvedBy: 'user-2', createdAt: '2026-05-05T14:00:00Z', updatedAt: '2026-05-06T10:00:00Z' },
  { id: 'rec-3', productId: 'prod-2', warehouseId: 'wh-2', systemCount: 32, physicalCount: 29, discrepancy: -3, reason: 'Suspected theft — under investigation', status: 'REJECTED', createdBy: 'user-4', approvedBy: null, createdAt: '2026-05-04T09:00:00Z', updatedAt: '2026-05-04T16:00:00Z' },
]

export const notifications = [
  { id: 'notif-1', userId: 'user-2', type: 'TRANSFER_SUGGESTION', message: 'New transfer suggestion: 20 Industrial Laptops from Warehouse Beta to Warehouse Alpha', referenceId: 'trans-1', isRead: false, createdAt: '2026-05-07T08:00:00Z' },
  { id: 'notif-2', userId: 'user-2', type: 'RECONCILIATION_REQUESTED', message: 'Reconciliation request for Printer Ink Cartridge at Warehouse Alpha needs your review', referenceId: 'rec-1', isRead: false, createdAt: '2026-05-07T11:00:00Z' },
  { id: 'notif-3', userId: 'user-5', type: 'RESTOCK_ALERT', message: 'Restock alert: Industrial Laptop at Warehouse Alpha is critically low (12 units, threshold: 20)', referenceId: 'alert-1', isRead: false, createdAt: '2026-05-07T08:10:00Z' },
  { id: 'notif-4', userId: 'user-3', type: 'TRANSFER_APPROVED', message: 'Transfer of 15 Office Chairs from Warehouse Beta approved — please confirm dispatch', referenceId: 'trans-2', isRead: true, createdAt: '2026-05-06T12:00:00Z' },
  { id: 'notif-5', userId: 'user-2', type: 'TRANSFER_SUGGESTION', message: 'New transfer suggestion: 25 Whiteboard Marker Sets from Warehouse Beta to Warehouse Alpha', referenceId: 'trans-3', isRead: true, createdAt: '2026-05-05T10:00:00Z' },
  { id: 'notif-6', userId: 'user-5', type: 'RESTOCK_ALERT', message: 'Restock alert: Office Chair at Warehouse Alpha needs restocking (8 units, threshold: 15)', referenceId: 'alert-3', isRead: false, createdAt: '2026-05-05T10:00:00Z' },
]

export const complaints = [
  { id: 'ticket-1', companyId: 'comp-1', companyName: 'TechWave Logistics', submittedBy: 'Bob Smith', email: 'manager@techwave.com', subject: 'Transfer suggestions are inaccurate', message: 'The system keeps suggesting unnecessary transfers. Our stock levels are adequate but alerts still trigger for products well above threshold.', priority: 'MEDIUM', status: 'OPEN', createdAt: '2026-05-06T10:00:00Z' },
  { id: 'ticket-2', companyId: 'comp-2', companyName: 'GlobalStock Inc.', submittedBy: 'Jane Cooper', email: 'jane@globalstock.com', subject: 'Cannot access reconciliation export', message: 'After the last update, managers can no longer export reconciliation reports. This is blocking our monthly audit process urgently.', priority: 'HIGH', status: 'OPEN', createdAt: '2026-05-07T09:00:00Z' },
  { id: 'ticket-3', companyId: 'comp-1', companyName: 'TechWave Logistics', submittedBy: 'Eve Martinez', email: 'procurement@techwave.com', subject: 'Alert notifications delayed', message: 'Restock alert notifications are arriving 2-3 hours late. We have missed critical restocking windows twice this week.', priority: 'HIGH', status: 'IN_REVIEW', createdAt: '2026-05-05T14:00:00Z' },
  { id: 'ticket-4', companyId: 'comp-4', companyName: 'Meridian Supplies', submittedBy: 'Mark Davis', email: 'mark@meridian.com', subject: 'Feature request: bulk warehouse import', message: 'It would be great to have an option to import multiple warehouses at once via CSV instead of adding them one by one. Would save us a lot of time.', priority: 'LOW', status: 'RESOLVED', createdAt: '2026-05-01T11:00:00Z' },
  { id: 'ticket-5', companyId: 'comp-3', companyName: 'Apex Traders', submittedBy: 'Sarah Green', email: 'sarah@apex.com', subject: 'Account suspension without notice', message: 'Our company account was suspended without any prior notification or warning. We have outstanding orders that need to be processed.', priority: 'HIGH', status: 'RESOLVED', createdAt: '2026-04-28T08:00:00Z' },
]

export const analytics = {
  stockVelocity: [
    { productId: 'prod-3', productName: 'Printer Ink Cartridge', sku: 'INK-003', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', velocityPerDay: 8.3, daysUntilEmpty: 8 },
    { productId: 'prod-4', productName: 'Whiteboard Marker Set', sku: 'MRK-004', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', velocityPerDay: 4.2, daysUntilEmpty: 5 },
    { productId: 'prod-1', productName: 'Industrial Laptop', sku: 'LAP-001', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', velocityPerDay: 2.5, daysUntilEmpty: 5 },
    { productId: 'prod-2', productName: 'Office Chair', sku: 'CHR-002', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', velocityPerDay: 1.8, daysUntilEmpty: 4 },
    { productId: 'prod-3', productName: 'Printer Ink Cartridge', sku: 'INK-003', warehouseId: 'wh-2', warehouseName: 'Warehouse Beta', velocityPerDay: 5.1, daysUntilEmpty: 23 },
    { productId: 'prod-1', productName: 'Industrial Laptop', sku: 'LAP-001', warehouseId: 'wh-2', warehouseName: 'Warehouse Beta', velocityPerDay: 1.2, daysUntilEmpty: 37 },
  ],
  lowStockForecast: [
    { productId: 'prod-2', productName: 'Office Chair', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', currentStock: 8, threshold: 15, daysUntilEmpty: 4, urgency: 'CRITICAL' },
    { productId: 'prod-4', productName: 'Whiteboard Marker Set', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', currentStock: 22, threshold: 30, daysUntilEmpty: 5, urgency: 'CRITICAL' },
    { productId: 'prod-1', productName: 'Industrial Laptop', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', currentStock: 12, threshold: 20, daysUntilEmpty: 5, urgency: 'CRITICAL' },
    { productId: 'prod-5', productName: 'Network Switch', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', currentStock: 6, threshold: 10, daysUntilEmpty: 7, urgency: 'CRITICAL' },
    { productId: 'prod-3', productName: 'Printer Ink Cartridge', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', currentStock: 65, threshold: 50, daysUntilEmpty: 8, urgency: 'WARNING' },
  ],
  reorderRecommendations: [
    { productId: 'prod-2', productName: 'Office Chair', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', suggestedQuantity: 55, recommendedOrderDate: '2026-05-09', avgVelocity: 1.8 },
    { productId: 'prod-1', productName: 'Industrial Laptop', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', suggestedQuantity: 50, recommendedOrderDate: '2026-05-10', avgVelocity: 2.5 },
    { productId: 'prod-4', productName: 'Whiteboard Marker Set', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', suggestedQuantity: 96, recommendedOrderDate: '2026-05-10', avgVelocity: 4.2 },
    { productId: 'prod-5', productName: 'Network Switch', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', suggestedQuantity: 28, recommendedOrderDate: '2026-05-11', avgVelocity: 0.8 },
  ],
  alertFrequency: [
    { productId: 'prod-1', productName: 'Industrial Laptop', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', alertCount: 5, lastAlertDate: '2026-05-07T08:10:00Z', avgDaysBetweenAlerts: 12.5 },
    { productId: 'prod-3', productName: 'Printer Ink Cartridge', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', alertCount: 4, lastAlertDate: '2026-05-06T09:00:00Z', avgDaysBetweenAlerts: 14.2 },
    { productId: 'prod-2', productName: 'Office Chair', warehouseId: 'wh-1', warehouseName: 'Warehouse Alpha', alertCount: 3, lastAlertDate: '2026-05-05T10:00:00Z', avgDaysBetweenAlerts: 18.3 },
  ],
  transferEfficiency: [
    { fromWarehouseId: 'wh-2', fromWarehouseName: 'Warehouse Beta', toWarehouseId: 'wh-1', toWarehouseName: 'Warehouse Alpha', totalSuggested: 8, totalAccepted: 5, totalRejected: 3, acceptanceRate: 62.5 },
    { fromWarehouseId: 'wh-1', fromWarehouseName: 'Warehouse Alpha', toWarehouseId: 'wh-2', toWarehouseName: 'Warehouse Beta', totalSuggested: 3, totalAccepted: 2, totalRejected: 1, acceptanceRate: 66.7 },
  ],
  movementSummary: [
    { summaryDate: '2026-05-01', totalIntake: 0, totalOutboundSales: 42, transfersIn: 0, transfersOut: 0 },
    { summaryDate: '2026-05-02', totalIntake: 120, totalOutboundSales: 28, transfersIn: 15, transfersOut: 0 },
    { summaryDate: '2026-05-03', totalIntake: 0, totalOutboundSales: 35, transfersIn: 0, transfersOut: 8 },
    { summaryDate: '2026-05-04', totalIntake: 80, totalOutboundSales: 45, transfersIn: 0, transfersOut: 0 },
    { summaryDate: '2026-05-05', totalIntake: 0, totalOutboundSales: 30, transfersIn: 8, transfersOut: 25 },
    { summaryDate: '2026-05-06', totalIntake: 50, totalOutboundSales: 22, transfersIn: 0, transfersOut: 15 },
    { summaryDate: '2026-05-07', totalIntake: 0, totalOutboundSales: 18, transfersIn: 0, transfersOut: 0 },
  ],
}
